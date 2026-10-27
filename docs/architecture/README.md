# OEssentials module architecture

The `refactor/minecraft-modulith` branch moves plugin composition, service ownership,
and shutdown into 56 MinecraftModulith modules. The Bukkit entry point retains the
119 public methods from version 6.8 as a compatibility facade.

## Dependency and build

The project builds with **Gradle** (Kotlin DSL) via the Gradle wrapper — Maven is no
longer used. MinecraftModulith is pinned to [`0.5.1`](https://github.com/OreoStudios/MinecraftModulith/tree/v0.5.1)
from the Oreo Studios Maven repository:

```kotlin
val modulithVersion = "0.5.1"
implementation("dev.oreo.modulith:modulith-paper:$modulithVersion")
annotationProcessor("dev.oreo.modulith:modulith-processor:$modulithVersion")
testImplementation("dev.oreo.modulith:modulith-test:$modulithVersion")
```

`modulith-processor` validates the new module boundaries during compilation;
`modulith-test` supplies the lifecycle test harness. The MinecraftModulith runtime,
ClassGraph, CommandsAPI, Libby and bStats are shaded and relocated into the plugin JAR
by the Shadow plugin. No separate MinecraftModulith server plugin is required.

The plugin targets **Java 21** bytecode. Because a provided dependency
(worldedit-bukkit 7.4.4, via the FastAsyncWorldEdit BOM) ships Java 25 bytecode, the
Gradle toolchain compiles on **JDK 25** with `--release 21`; the tests run on JDK 25
with `-Dnet.bytebuddy.experimental=true` for Mockito. Gradle auto-provisions the
toolchain if needed.

Build with:

```bash
./gradlew clean build      # compile, test, and produce the shaded jar
./gradlew shadowJar        # just the plugin jar
```

The distributable artifact is `build/libs/OEssentials-7.0.jar` (the
`-plain.jar` beside it is the unshaded classes and is not used for deployment).

> Note: `build.gradle.kts` lists `mavenLocal()` first so the build still succeeds when
> a remote plugin repository is briefly unreachable (e.g. `maven.devs.beer` returning
> HTTP 522) and the artifacts are already in the local Maven cache.

## Layout

| Location | Responsibility |
| --- | --- |
| `OEssentials` | Bukkit entry point, library loading, compatible public getters and reload methods |
| `bootstrap/OreoModules` | Explicit catalog of all lifecycle modules; no runtime package scanning |
| `bootstrap/ManagedModule` | Named service publication, startup rollback, resource ownership and event subscriptions |
| `bootstrap/features/<feature>` | Feature initialization, private state, named service API and cleanup |
| `bootstrap/OreoModuleRegistry` | Compatibility lookup for callers of the old plugin API |
| `util/ModuleTaskScope` | Ownership of scheduled tasks created during initialization and their nested callbacks |
| Existing `modules`, `services`, `commands`, `api` packages | Gameplay implementation and established external API types |

New composition modules consume other modules through interfaces annotated
`@ModuleApi("services")` and declare those dependencies in `@PluginModule`.
MinecraftModulith enforces those service lookups and validates the graph. Existing
gameplay classes continue using their established APIs; the annotation processor's
module boundaries cover `bootstrap/features`, not all legacy gameplay packages.

The explicit ordering dependencies retain the established initialization sequence,
including constructor side effects. Reverse shutdown keeps storage and configuration
available until their consumers finish. See [the complete dependency graph](modules.mmd).

## Lifecycle

1. `onLoad` loads the existing external libraries.
2. `onEnable` starts `PaperModulith` with the explicit catalog.
3. Each module publishes its named service interface before constructing its managers.
   This preserves getters used by manager constructors. Disabled feature flags still
   leave their manager getters null, as before.
4. Cleanup is registered before resource creation. Startup failures unwind both the
   partially initialized module and previously started modules.
5. Module shutdown cancels owned tasks, unregisters listeners/events, and releases
   resources. Cleanup failures are reported without skipping remaining cleanup.
6. Global plugin shutdown removes residual Bukkit registrations and clears the
   compatibility registry.

`OreScheduler` keeps its Paper/Folia global, location, entity, and async scheduling
behavior. Module ownership propagates through callbacks and nested scheduling.
Completed one-shot tasks are released from tracking. Tasks started outside module
initialization/callbacks retain the existing plugin-managed lifetime. This refactor
does not introduce independent hot unloading of gameplay modules.

## Asynchronous messaging

Messaging starts locally and connects to RabbitMQ asynchronously. Connecting senders
are owned before the connection completes and are closed if the plugin stops.
Activation callbacks check module activity before touching services.

After the packet manager is initialized, the module publishes `MessagingReady` on
the server scheduler. Currency, portals, RTP, and group RTP install their own handlers
through `@ModuleListener`; subscriptions disappear when their modules stop. Messaging
no longer reads later modules through the compatibility registry.

## Module boundary enforcement

A feature module is **protected** when its root `package-info.java` declares
`@ApplicationModule` with the same id as its `@FeaturePackage` and lifecycle class:

```java
@dev.oreo.modulith.core.ApplicationModule(id = "rules")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "rules", lifecycle = fr.elias.oessentials.rules.lifecycle.RulesModule.class)
package fr.elias.oessentials.rules;
```

The annotation processor then treats the whole feature package as one module.
Another module that references any class in it (other than a `@ModuleApi` type it
has declared a dependency on) fails to compile:

```text
error: Module 'help' cannot access internal type fr.elias.oessentials.rules.internal.RulesCommand from module 'rules'
```

Unprotected modules are only checked at their `lifecycle` package, so their
`internal` packages can still be used from anywhere. Lifecycle dependencies stay
on `@PluginModule`, and `@ApplicationModule` only declares the boundary.

### Rollout

Modules are protected one migration at a time. Each migration replaces other modules'
use of a provider's internals with a deliberate contract (`@ModuleApi` interfaces and
DTOs in the provider's `api` package, with dependency inversion where the direction is
wrong), then adds `@ApplicationModule` to that provider. Internal classes are not moved
into global packages to silence the checks.

`ModuleBoundaryRolloutTest.PROTECTED` lists the protected modules. It can only grow,
and the test fails if a module loses its annotation. Measure what is left with:

```bash
python tools/count-boundary-violations.py            # total, grouped by provider
python tools/count-boundary-violations.py --details  # also consumer -> provider pairs
```

The script temporarily protects every remaining module, compiles, and restores the
files.

| Migration | Protected | Remaining violations | Notes |
| --- | --- | --- | --- |
| Baseline (MinecraftModulith 0.5.1) | 0 / 56 | 1,548 | All are `internal` references across 219 consumer → provider pairs |
| 1. Modules nothing depends on | 6 / 56 | 1,548 | `discord-integration`, `events`, `help`, `motd`, `mute-system`, `rules` had no inbound references |

The largest providers are `messaging` (366), `storage` (188), `configuration` (177) and
`player-services` (108). Their public contracts should be designed before their consumers
are migrated.

## Compatibility and diagnostics

Existing configuration paths, feature flags, persisted data formats, command names,
and public API classes remain in place. The custom-tab visibility fix from 6.8 is
included. A running lifecycle module does not imply that its gameplay feature flag
is enabled.

`getModuleDiagnostics()` exposes MinecraftModulith's state/metrics snapshot while
the plugin is running. Compile-time module metadata is packaged under
`META-INF/minecraft-modulith/modules.idx`.

Tests cover graph/catalog consistency, all 119 released entry-point signatures,
dependency shutdown order, startup/linkage failure rollback, cleanup errors,
event unsubscription, task ownership/races, and command cleanup identity. Existing
feature tests remain part of the build. These checks do not replace live Paper/Folia
testing with the server's optional integrations.

Before deployment, verify join/quit, late joins with the custom tab layout, world
changes, configuration reloads, storage writes at shutdown, and RabbitMQ connecting
or failing after startup on a test server with the production configuration.
