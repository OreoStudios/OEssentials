package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.command.CommandSender;

/** Server identity, debug logging, and the storage and homes keys of {@code config.yml}. */
@ModuleApi("services")
public interface ServerConfig {
    /** {@code server.name}: this server's name on the network. */
    String serverName();

    boolean isDebugEnabled();

    int tpaTimeoutSeconds();

    boolean mongoEnabled();
    String mongoUri();
    String mongoDatabase();
    String mongoPrefix();

    int defaultMaxHomes();
    boolean permissionBased();

    /** The highest {@code oreo.homes.<n>} permission, or {@link #defaultMaxHomes()}. */
    int getMaxHomesFor(CommandSender sender);
}
