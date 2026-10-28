package fr.elias.oessentials.mail.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.mail.internal.rabbitmq.MailDeliveryPacket;
import fr.elias.oessentials.mail.internal.rabbitmq.MailPacketNamespace;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;

@PluginModule(value = "mail", dependencies = {"configuration::services", "messaging::services"}, after = {"rules"})
public final class MailModule extends ManagedModule implements MailServices {
    private fr.elias.oessentials.mail.internal.MailService mailService;
    private fr.elias.oessentials.mail.internal.MailListener mailListener;

    @Override
    protected void start() {
        services(MessagingServices.class).registerNamespace(new MailPacketNamespace());
        initMail();
        if (mailListener != null) {
            // Mail delivery notifications from other servers
            services(MessagingServices.class).subscribe(MailDeliveryPacket.class, (channel, pkt) -> {
                try { mailListener.onMailDelivery(pkt); }
                catch (Throwable t) { plugin.getLogger().warning("[Mail] Failed to handle MailDeliveryPacket: " + t.getMessage()); }
            });
        }
    }

    @Override public fr.elias.oessentials.mail.internal.MailService getMailService() { return mailService; }
    @Override public fr.elias.oessentials.mail.internal.MailListener getMailListener() { return mailListener; }

    private void initMail() {
        try {
            this.mailService  = new fr.elias.oessentials.mail.internal.MailService(plugin);
            this.mailListener = new fr.elias.oessentials.mail.internal.MailListener(plugin, this.mailService);
            var mailCmd       = new fr.elias.oessentials.mail.internal.MailCommand(plugin, this.mailService);
            services(ConfigurationServices.class).getCommands().register(mailCmd);
            plugin.getServer().getPluginManager().registerEvents(this.mailListener, plugin);
            plugin.getLogger().info("[Mail] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Mail] Failed to initialize: " + t.getMessage());
        }
    }
}
