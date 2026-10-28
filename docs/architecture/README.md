# OEssentials module architecture

The `refactor/minecraft-modulith` branch moves plugin composition, service ownership,
and shutdown into 56 MinecraftModulith modules. The Bukkit entry point retains the
119 public methods from version 6.8 as a compatibility facade.

## Dependency and build

The project builds with **Gradle** (Kotlin DSL) via the Gradle wrapper — Maven is no
longer used. MinecraftModulith is pinned to [`0.5.3`](https://github.com/OreoStudios/MinecraftModulith/tree/v0.5.3)
from the Oreo Studios Maven repository:

```kotlin
val modulithVersion = "0.5.3"
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

`messaging` is the RabbitMQ transport and depends only on `configuration`, so it starts
right after it. Every module that sends or receives packets depends on it and plugs in its
own cross-server code:

1. While starting, a module calls `MessagingServices.registerNamespace(...)` for its packets
   and `MessagingServices.subscribe(...)` for its handlers. Both are applied when RabbitMQ
   connects, before any packet is dispatched.
2. Messaging connects asynchronously. Connecting senders are owned before the connection
   completes and are closed if the plugin stops.
3. On the main thread it then publishes `MessagingReady`; modules build their brokers in an
   `@ModuleListener` (player-services: tpa/tp/back and spawn/warp; trade; storage: homes and
   player warps; inventory-sync: invsee; mod-gui: the moderation bridge; afk; currency;
   portals; RTP; group RTP). Without RabbitMQ, or if it cannot connect, the event never
   fires and those modules stay local.

`PacketManager` stops dispatching as soon as the plugin starts disabling, so no handler runs
against a module that has already shut down even though messaging now stops after them.

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
| 2a. Configuration settings contract | 6 / 56 | 1,408 | `configuration` 177 → 37; see below |
| 2b. Configuration protected | 7 / 56 | 1,313 | Non-configuration features moved to their owners; `messaging` 366 → 344, `storage` 188 → 173 |
| 3a. Messaging packet contract | 7 / 56 | 1,241 | `messaging` internals 344 → 44; 228 references now fail only because their module does not declare `messaging` (see below) |
| 3b. Messaging protected | 8 / 56 | 825 | Dependency direction reversed; cross-server code moved to its owners; no undeclared dependencies left |
| 4. Storage protected | 9 / 56 | 645 | Commands registered by storage; contracts `PlayerLocator`, `Homes`, `PlayerWarps`, `BackDataStore`, `StorageDialogs` port, public `IWarpsAPI`/`ISpawnAPI`/`IPlayerWarpsAPI` |
| 5. Commands moved to their owners | 11 / 56 | 562 | 16 modules register their own commands; `group-rtp` and `custom-worlds` protected |
| 6. Public API published by its owners | 11 / 56 | 442 | Each module provides its `fr.elias.oessentials.api` object through `PublicApis`; integrations no longer builds them |
| 7. MinecraftModulith 0.5.2 | 20 / 56 | 441 | Processor no longer reports false errors for lambdas over wildcards; 9 modules that no one reaches into are protected |
| 8. Web panel, reload hooks | 28 / 56 | 340 | `ReloadSteps`, `ConfigFiles`, `MarketOrders`, `ShopSales`, `AfkStatusSink`; ItemBuilder and item hooks in platform |
| 9. Small providers, MinecraftModulith 0.5.3 | 35 / 56 | 275 | Proxy via `ServerConnector`, redis `BalanceCache`, item codec in messaging API, service-level contracts; mod-gui `uses` ender-chest/freeze |
| 10. Codec, respawn, proxy and redis leftovers | 39 / 56 | 266 | `ItemStacksCodec` in the messaging API; respawn listener in player-services |
| 11. Moderation notifier, public API home | 40 / 56 | 245 | Discord `ModerationNotifier` (reached with `uses`); OEssentialsAPI assembled and registered by configuration |
| 12. Placeholders answered by their modules | 42 / 56 | 214 | `Placeholders` registry; kits, playtime, vault and balance placeholders moved to their owners |
| 13. Aliases and dialogs moved to their features | 44 / 56 | 184 | Aliases owned by command-control; feature dialogs in their features with `platform/ui/Dialogs` |
| 14. Clone, kits count, currency placeholders, economy lookups | 45 / 56 | 169 | /clone in skins; currency registers its PAPI expansion; economy lookups on `EconomyServices` |
| Mute owns its service; chat/notes/mod-gui contracts | 48 / 56 | 132 | MuteService → mute-system api, MuteListener → mute internal; mod-gui ChatControls; notes openNotes/isWaitingForNote; CustomConfig → platform.config; IpTracker owned by mod-gui |
| Last contracts; every module protected | 56 / 56 | 0 | Currencies/Currency, InventoryService/InvseeSnapshots, ShopSales.listedSellPrice, InventoriesServices.consumeChatInput, BackLocation → player api, NametageCondition → platform.config, vanish via IVanishAPI; /price → shop; AutoMessageScheduler → chat |

The largest providers are now `storage` (160), `player-services` (76), `shop` (58) and `inventories` (54).
Their public contracts should be designed before their consumers are migrated.

### Configuration contract

`fr.elias.oessentials.configuration.api` holds what other modules may read:

| Type | Source | Replaces |
| --- | --- | --- |
| `FeatureSettings` | `config/settings.yml` feature switches and options | `SettingsConfig` |
| `PerformanceGuardSettings` | the `performance.*` section of `settings.yml` | `SettingsConfig` |
| `ServerConfig` | server name, debug flag, storage and homes keys of `config.yml` | `ConfigService` |
| `CrossServerFeatures` | `features.cross-server.*` | `CrossServerSettings` |
| `AutoReboot` | scheduled restarts (extends the public `IAutoRebootAPI`) | `AutoRebootService` |

Tempfly is published as the public `ITempFlyAPI` (`ConfigurationServices.getTempFly()`).

Modules that start after configuration plug into it through ports it publishes, instead of
configuration depending on them:

| Port | Registered by | Used for |
| --- | --- | --- |
| `SafeZoneRelay` | `messaging` | auto-reboot sending players to the safe-zone server; messaging also owns the reboot packet namespace (wire ids unchanged) and forwards arrivals to `AutoReboot` |
| `BossBarToggles` | `nametag` | the multi-bossbar switches in `/oesettings` |

`storage` does the same for the importers: economy registers a `BalanceImportTarget`.

`ConfigurationServices` returns these types and owns writes: `reloadSettings()` reloads
`settings.yml` and the cross-server switches, `updateSettings(Map)` writes keys (keeping the
legacy `chat.banned-words` section in sync), saves and reloads. Inside a module, use
`OEssentials.getFeatureSettings()`, `getPerformanceGuardSettings()`, `getServerConfig()` and
`getCrossServerFeatures()`. The released getters (`getSettingsConfig()`, `getSettings()`,
`getConfigService()`, `getCrossServerSettings()`) still return the same objects for other plugins.

`ModuleBoundaryRolloutTest.featureApiPackagesOnlyPublishContracts` fails if a type in any
feature's `api` package is not `@ModuleApi` or its methods expose that feature's implementation.

Features that lived in `configuration` without being configuration moved to their owners
before it was protected, because several of them depended on modules that start after it:

| Feature | Now in |
| --- | --- |
| Freeze state, its listener and `/freeze` | `freeze` (public API via `FreezeServices.getFreezeApi()`) |
| Killall log, `/killallr`, `/killallrlog` | `mobs` |
| CMI, EssentialsX and zEssentials importers | `storage` |
| `/invlook` | `messaging`, next to invsee |
| `CraftActionsConfig` | `custom-crafting` |
| `/oesettings` command | `configuration` (moved in from `commands`) |

The released `OEssentials` getters for these keep their names; where a class moved package,
its return type changed with it (recorded in `class-relocations.json`).

### Messaging contract

The packet framework is messaging's published contract in `fr.elias.oessentials.messaging.api`:
`Packet`, `FriendlyByteInputStream`/`FriendlyByteOutputStream`, `PacketChannel`/`PacketChannels`,
`PacketNamespace`/`PacketDefinition`/`PacketProvider`, `PacketSubscriber` and `PacketBus` (implemented
by the internal `PacketManager`). Modules get the bus from `MessagingServices.getPacketBus()`,
`MessagingReady.packets()` or `OEssentials.getPacketBus()`; the released `getPacketManager()` remains.

`SendRemoteMessagePacket` and `NetworkPlayerCounts` (proxy player counts) are part of the
contract too.

Packet ids are on the wire. Until 7.1.6, 23 packets got their ids (27006-27028) from the order
`MessagingModule` registered them at startup; they now have exactly those ids as fixed ids in
namespaces next to the packets, so the wire format is unchanged. `PacketWireIdsTest` pins every
id and fails if a `PacketNamespace` is not passed to `registerNamespace` exactly once; the
registry fails startup on a duplicate packet, class or namespace id.

Features that lived in messaging but belonged elsewhere moved to their owners: invsee and
invlook to `inventory-sync`, the moderation bridge to `mod-gui`, join/quit cache sync to
`economy`, death messages to `events`, `/msg` to `player-services`, and the chat join/quit
listeners to `chat`. The unused `InvBridge` was removed. Auto-reboot reaches the proxy through
a `ServerConnector` port that proxy-messaging registers with configuration.

`docs/architecture/modules.mmd` is generated: run `python tools/generate-module-graph.py`
after changing a `@PluginModule` (`--check` reports a stale file).

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
