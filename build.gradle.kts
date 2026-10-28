plugins {
    java
    // Maintained fork of the Shadow plugin (johnrengelman is unmaintained); supports Gradle 9.
    id("com.gradleup.shadow") version "9.0.0"
}

// group / version come from gradle.properties

java {
    // Some provided dependencies (worldedit-bukkit 7.4.4 via the FAWE BOM) ship
    // Java 25 bytecode, so javac must run on JDK 25 to read them — while the
    // plugin itself still targets Java 21 (see options.release below).
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Emit Java 21 bytecode so the plugin runs on Java 21 servers.
    options.release.set(21)
    options.encoding = "UTF-8"
}

repositories {
    // Resolve from the local Maven cache first — lets the build proceed when a
    // remote plugin repo is temporarily unreachable (e.g. maven.devs.beer 522).
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://raw.github.com/dominicfeliton/SmartInvs/bukkit-maven-repo")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.oreostudios.fr/oreostudioslib")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://maven.devs.beer/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.groupez.dev/snapshots")
    maven("https://repo.groupez.dev/releases")
    maven("https://repo.triumphteam.dev/releases/")
    maven("https://repo.triumphteam.dev/snapshots/")
    maven("https://repo.lushplugins.org/releases")
    maven("https://repo.viaversion.com/")
    maven("https://repo.opencollab.dev/main/")
}

// Mirror Maven's 'provided' scope: compileOnly dependencies are also visible to tests.
configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

val modulithVersion = "0.5.3"

dependencies {
    // ── MinecraftModulith (shaded + relocated) ──────────────────────────────
    implementation("dev.oreo.modulith:modulith-paper:$modulithVersion")
    annotationProcessor("dev.oreo.modulith:modulith-processor:$modulithVersion")
    testImplementation("dev.oreo.modulith:modulith-test:$modulithVersion")

    // ── Bundled runtime libraries (shaded into the plugin jar) ──────────────
    implementation("fr.traqueur.commands:platform-spigot:5.2.1")
    implementation("de.oliver:FancySitula:0.0.13")
    implementation("de.oliver:FancyLib:36")
    implementation("de.oliver.FancyAnalytics:api:0.1.6")
    implementation("de.oliver.FancyAnalytics:logger:0.0.6")
    implementation("org.lushplugins:ChatColorHandler:5.1.3")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("net.byteflux:libby-bukkit:1.3.1")
    implementation("dev.triumphteam:triumph-gui:3.1.13")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("fr.minuskube.inv:bukkit-smart-invs:1.3.5")

    // ── Provided by the server / other plugins at runtime (compile-only) ────
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("de.oliver:FancyNpcs:2.4.4")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.1.0-SNAPSHOT")
    compileOnly("org.mongodb:mongodb-driver-sync:5.1.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.nexomc:nexo:1.15.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
    compileOnly("dev.lone:api-itemsadder:4.0.10")
    compileOnly("redis.clients:jedis:5.1.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.rabbitmq:amqp-client:5.15.0")
    compileOnly("org.postgresql:postgresql:42.7.4")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.3")

    // FastAsyncWorldEdit version is managed by the IntellectualSites BOM.
    compileOnly(platform("com.intellectualsites.bom:bom-newest:1.55"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")

    // Local system jar (kept in libs/ — no public repository).
    compileOnly(files("libs/UltimateChristmas-3.2-SNAPSHOT.jar"))

    // ── Tests ───────────────────────────────────────────────────────────────
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Prevent a transitive SNAPSHOT of triumph-gui (via de.oliver.*) from winning.
configurations.all {
    resolutionStrategy.force("dev.triumphteam:triumph-gui:3.1.13")
}

tasks.processResources {
    // Expand ${version} in plugin.yml (Maven resource filtering equivalent).
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    // Produce OEssentials-<version>.jar as the primary artifact (like maven-shade).
    archiveClassifier.set("")
    // Project resources are added first, so first-wins keeps our plugin.yml and
    // drops the copy bundled inside SmartInvs.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
    relocate("dev.oreo.modulith", "fr.elias.oessentials.libs.modulith")
    relocate("io.github.classgraph", "fr.elias.oessentials.libs.classgraph")
    relocate("fr.traqueur.commands", "fr.elias.oessentials.libs.commands")
    relocate("net.byteflux.libby", "fr.elias.oessentials.libs.libby")
    relocate("org.bstats", "fr.elias.oessentials.libs.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
    // Mockito/ByteBuddy need this to instrument on the JDK 25 test runtime.
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}
