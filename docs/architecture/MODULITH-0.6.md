# MinecraftModulith 0.6.0 in OEssentials

OEssentials uses 0.6.0 for the Paper runtime, annotation processor, test harness,
and Gradle plugin. All 56 feature modules retain their existing IDs and lifecycle
ordering. Existing public OEssentials interfaces and gameplay configuration are preserved.

## Typed factories and identities

`OreoModules` registers each feature through `PaperModulith.Builder.functor` with
a typed `ModuleEnvironment` (the plugin and its compatibility registry). Factories
use constructor references and run only when the module's dependencies are ready.

`ManagedModule` publishes its compatibility API under a `ServiceKey` using the
runtime instance identity. Named instances therefore do not replace one another's
services or remove one another's registrations during shutdown. Unqualified legacy
getters still return null for absent features, but reject ambiguous providers.
The integration test exercises two named copies of one template with `bind`,
`dependsOn`, qualified service lookup, and independent lifecycle cleanup.

Production gameplay modules remain singletons: creating duplicate economy,
command, or inventory managers would also duplicate their Bukkit registrations
and persistence. Named instance support is available for features designed to
own independent resources; it is not a switch for duplicating singleton gameplay.

## API boundaries

The 16 existing `uses` declarations now live on `@ApplicationModule` package
boundaries. Required startup dependencies remain on `@PluginModule` lifecycle
classes. The 0.6.0 processor and runtime merge these declarations; tests compare
the generated metadata against the runtime catalog so access and ordering cannot drift.

## Events and Folia

The ten `MessagingReady` handlers register through `PaperEventRoutes.global`.
They coordinate transport subscriptions; entity/region work continues to use its
own scheduler. Routed callbacks also restore `ModuleTaskScope`, so nested legacy
scheduled work is still cancelled with the owning module.

Module event publication returns `publishAsync`'s completion stage. It never
joins a scheduler's own queued work. Readiness failures are logged when delivery
actually completes. `ManagedModule` also exposes scoped region, entity and async
event registration helpers, including 0.6.0's retired-entity failure behavior.
Route target selectors must use captured locations or entity references, without
reading region-owned state from an arbitrary publisher thread.

`MessagingReady` contains a live `PacketBus`, so it is transient and is not
persisted or retried from disk. The newer event replay matching and serialized
retry coordinator are supplied by the upgraded runtime, but persistent business
events require serializable payloads, an adapter, and idempotent handlers. No extra
database or distributed exactly-once guarantee is enabled by this upgrade.

## Reports and diagnostics

At startup the plugin writes `modulith/architecture.json`, `architecture.md`, and
`modules.mmd` under its data folder. These reports use the validated runtime catalog
and include instance IDs, template IDs, dependencies, uses, and ordering.
`OEssentials.getModuleCatalog()` exposes the same immutable catalog.

The framework's `/modulith` diagnostics use the `minecraftmodulith.admin`
permission (default op). `gradlew verifyModulith modulithDocs` validates the compiled
index and writes declaration reports to `build/reports/modulith`. Both tasks are
part of the normal build. The 0.6.0 report tasks capture Gradle's Project, so they
are explicitly marked incompatible with configuration caching; other task builds
can continue using OEssentials' configuration cache.

## Server validation

Automated checks cover module boundaries, factories, named service isolation,
asynchronous stage completion, reports, and lifecycle rollback. A live Paper/Folia
smoke run is still required to validate optional plugin integrations and server
behavior: enable RabbitMQ, wait for readiness, join/quit players, run diagnostics,
and stop the server while deliveries are queued. Do not block a region/entity
thread waiting on routed completion.
