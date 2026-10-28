package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A group of packets with fixed ids. Ids are on the wire: never renumber a packet that has shipped.
 */
@ModuleApi("services")
public abstract class PacketNamespace {

    private final short namespaceId;
    private final Set<PacketDefinition<?>> definitions =
            ConcurrentHashMap.newKeySet();

    private volatile boolean registered = false;

    protected PacketNamespace(short namespaceId) {
        this.namespaceId = namespaceId;
    }

    /**
     * Subclasses call this inside {@link #registerPackets()} to add packets.
     */
    protected final <T extends Packet> void registerPacket(
            int packetId,
            Class<T> packetClass,
            PacketProvider<T> provider
    ) {
        PacketDefinition<T> def = new PacketDefinition<>(packetId, packetClass, provider, this);
        definitions.add(def);
    }

    /**
     * Implementations must register all their packets here via {@link #registerPacket(...)}.
     * Do NOT call this directly; it's invoked lazily by {@link #ensureRegistered()}.
     */
    protected abstract void registerPackets();

    /**
     * Ensures {@link #registerPackets()} has been called exactly once.
     */
    public final void ensureRegistered() {
        if (!registered) {
            synchronized (this) {
                if (!registered) {
                    registerPackets();
                    registered = true;
                }
            }
        }
    }

    public final short getNamespaceId() {
        return namespaceId;
    }

    /**
     * Returns all definitions for this namespace.
     * Lazily triggers {@link #registerPackets()} on first call.
     */
    public final Collection<PacketDefinition<?>> getDefinitions() {
        ensureRegistered();
        return definitions;
    }

}
