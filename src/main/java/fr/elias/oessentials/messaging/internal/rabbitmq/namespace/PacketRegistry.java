package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.PacketDefinition;
import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.api.PacketProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class PacketRegistry {

    private final Map<Class<? extends Packet>, PacketDefinition<? extends Packet>> classToDefinition =
            new ConcurrentHashMap<>();
    private final Map<Long, PacketDefinition<?>> idToDefinition =
            new ConcurrentHashMap<>();
    private final Set<Short> namespaceIds = ConcurrentHashMap.newKeySet();

    private final PacketNamespace dynamicNamespace = new DynamicPacketNamespace();

    private final AtomicInteger nextDynamicId;

    /** Registers every namespace; a packet id, class or namespace id used twice is a startup error. */
    public PacketRegistry(Collection<? extends PacketNamespace> namespaces) {
        for (PacketNamespace namespace : namespaces) {
            register(namespace);
        }

        int start = idToDefinition.keySet().stream()
                .mapToInt(k -> (int) (long) k)
                .max()
                .orElse(999) + 1;
        this.nextDynamicId = new AtomicInteger(start);
    }

    public <T extends Packet> void register(PacketDefinition<T> definition) {
        PacketDefinition<?> byId = idToDefinition.putIfAbsent(definition.getRegistryId(), definition);
        if (byId != null && byId != definition) {
            throw new IllegalStateException("Packet id " + definition.getRegistryId() + " is used by both "
                    + byId.getPacketClass().getName() + " and " + definition.getPacketClass().getName());
        }
        PacketDefinition<?> byClass = classToDefinition.putIfAbsent(definition.getPacketClass(), definition);
        if (byClass != null && byClass != definition) {
            throw new IllegalStateException(definition.getPacketClass().getName() + " is registered with ids "
                    + byClass.getRegistryId() + " and " + definition.getRegistryId());
        }
    }

    /** Adds a namespace; a namespace id used twice is an error. */
    public void register(PacketNamespace namespace) {
        if (!namespaceIds.add(namespace.getNamespaceId())) {
            throw new IllegalStateException("Packet namespace id " + namespace.getNamespaceId() + " is registered twice");
        }
        for (PacketDefinition<?> definition : namespace.getDefinitions()) {
            register(definition);
        }
    }

    public PacketDefinition<? extends Packet> getDefinition(Class<? extends Packet> packetClass) {
        return classToDefinition.get(packetClass);
    }

    public PacketDefinition<?> getDefinition(long registryId) {
        return idToDefinition.get(registryId);
    }

    /**
     * Programmatically register a packet class with a constructor supplier.
     * If the class is already registered, this is a no-op.
     *
     * <p>The id depends on registration order, so prefer a {@link PacketNamespace} with fixed ids.</p>
     */
    public <T extends Packet> void register(Class<T> packetClass, Supplier<T> constructor) {
        @SuppressWarnings("unchecked")
        PacketDefinition<T> existing = (PacketDefinition<T>) classToDefinition.get(packetClass);
        if (existing != null) return;

        int id = nextDynamicId.getAndIncrement();

        PacketProvider<T> provider = constructor::get;

        PacketDefinition<T> def = new PacketDefinition<>(
                id,
                packetClass,
                provider,
                dynamicNamespace
        );

        classToDefinition.put(packetClass, def);
        idToDefinition.put((long) id, def);
    }
}
