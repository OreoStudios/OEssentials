# Connecting the web panel without RabbitMQ

Install a backend and plugin version that include the WebSocket transport. Register the server in the panel, copy its API key, and set these values in the plugin's `config.yml`:

```yaml
web-panel:
  enabled: true
  transport: websocket
  url: "https://api.oreostudios.fr"
  api-key: "YOUR_SERVER_API_KEY"
```

Restart the Minecraft server. The owner panel's connection indicator shows WebSocket when connected. Do not paste a RabbitMQ URI into the panel for this mode. The plugin initiates the connection; no Minecraft-side inbound port is needed. Keep HTTPS for production so the API key and commands travel over TLS.

Live config patches, config fetch/push, LuckPerms updates, inventory actions, deliveries, and achievement notifications use the WebSocket command channel. Player sync, metrics, AFK updates, orders, and config file uploads use the authenticated HTTP API. Config fetch uploads replace the panel's previous copy; uploads are limited to 2 MiB of text.

The plugin reconnects after disconnects, with retry backoff up to 60 seconds. HTTP sync and existing inventory/delivery polling continue while the socket is unavailable. Configuration and permission commands are not durably queued by WebSocket: check the panel's result and retry a failed push after reconnecting. A successful push means it was sent, not that every game-side operation has completed. Achievement notifications missed while disconnected remain recorded in the backend but are not replayed in chat.

The backend supports one WebSocket connection per registered server. A new connection replaces the old one. Do not reuse one API key on multiple Minecraft servers. Disabling a server or rotating its API key invalidates its connection.

## Optional RabbitMQ mode

Set `web-panel.transport: rabbitmq` to keep the original transport. It uses `rabbitmq.enabled` and `rabbitmq.uri`; the panel must have the matching broker credentials. An older config without `web-panel.transport` retains RabbitMQ when `rabbitmq.enabled` is true; otherwise it selects WebSocket. Explicit WebSocket mode does not disable RabbitMQ used by the plugin's other cross-server features.

## Self-hosted backend

Forward WebSocket upgrades at `/api/v1/plugin/connect` to the same backend that handles the panel API. The API-key header is sent in the upgrade request, never in the URL. A typical nginx location is:

```nginx
location /api/v1/plugin/connect {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 90s;
}
```

Sessions are held in the backend process. This implementation requires a single backend instance, or routing all commands for a server to the instance owning its socket. Arbitrary multi-instance load balancing needs shared command routing before enabling this transport.
