package fr.elias.oessentials.storage.lifecycle;

import com.mongodb.client.MongoClient;
import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.storage.api.BalanceImportTarget;
import fr.elias.oessentials.storage.internal.homes.home.HomeDirectory;
import fr.elias.oessentials.storage.internal.homes.home.HomeService;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpDirectory;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpService;
import fr.elias.oessentials.storage.internal.spawn.SpawnDirectory;
import fr.elias.oessentials.storage.internal.spawn.SpawnService;
import fr.elias.oessentials.storage.internal.warps.WarpService;
import fr.elias.oessentials.storage.internal.warps.rabbit.WarpDirectory;
import fr.elias.oessentials.storage.internal.services.StorageApi;

@ModuleApi("services")
public interface StorageServices {
    MongoClient getHomesMongoClient();
    HomeDirectory getHomeDirectory();
    StorageApi getStorage();
    SpawnService getSpawnService();
    WarpService getWarpService();
    HomeService getHomeService();
    PlayerWarpService getPlayerWarpService();
    PlayerWarpDirectory getPlayerWarpDirectory();
    WarpDirectory getWarpDirectory();
    SpawnDirectory getSpawnDirectory();
    org.bukkit.configuration.file.FileConfiguration getPlayerWarpsConfig();
    fr.elias.oessentials.storage.internal.directory.PlayerDirectory getPlayerDirectory();
    void reloadPlayerWarpsConfig();

    /** Registers where imported balances are written, or {@code null} when no economy is running. */
    void setBalanceImportTarget(BalanceImportTarget target);

    fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker();

    /** Released facade type; never created. */
    fr.elias.oessentials.storage.internal.homes.TeleportBroker getTeleportBroker();

    /** {@code null} without MongoDB storage. */
    fr.elias.oessentials.storage.api.PlayerLocator getPlayerLocator();
    fr.elias.oessentials.storage.api.Homes getHomes();
    /** {@code null} when player warps are disabled. */
    fr.elias.oessentials.storage.api.PlayerWarps getPlayerWarps();
    fr.elias.oessentials.storage.api.BackDataStore getBackDataStore();
    fr.elias.oessentials.api.IWarpsAPI getWarpsApi();
    fr.elias.oessentials.api.ISpawnAPI getSpawnApi();
    /** {@code null} when player warps are disabled. */
    fr.elias.oessentials.api.IPlayerWarpsAPI getPlayerWarpsApi();

    /** Registers the dialogs storage's commands open, or {@code null} to remove them. */
    void setDialogs(fr.elias.oessentials.storage.api.StorageDialogs dialogs);

    /** The registered dialogs, or {@code null}. */
    fr.elias.oessentials.storage.api.StorageDialogs getDialogs();
}
