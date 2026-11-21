# CUSTOM TAB dynamic rows and empty-slot skins

Edit `plugins/OEssentials/config/scoreboard-tab/tab.yml`:

```yaml
tab:
  layout-mode: CUSTOM
  custom-layout:
    empty-slots:
      mode: DYNAMIC
      min-rows: 5
      skin:
        enabled: false
        texture: ''
        signature: ''
```

- `LEGACY` (default) preserves the pre-existing layout: the multi-column renderer always uses 20 slots per column.
- `DYNAMIC` reserves at least `max(online players, min-rows)` **player positions** distributed over active columns, plus existing decoration rows. Use `min-rows: 0` to follow the current online player count without unnecessary player padding.
- `FIXED` treats `min-rows` as the minimum **total** row count per column, including decorative rows, expanding if players require more.
- `FILL` uses 20 rows per active column, including vacant positions.

To replace the default Steve/Alex heads of *empty player positions*, supply a Mojang base64 `textures` value in `skin.texture`, optionally its `signature`, then set `skin.enabled: true`. Real player skins and decoration rows are unchanged.

**Limits:** Vanilla Minecraft draws at most 20 rows per column. A 2-column layout needs at least 21 total tab entries and 3 columns needs at least 41; therefore all columns except the last are padded to 20 rows, and only the final column can shrink dynamically. This means a forced 2/3-column layout may retain empty positions even in DYNAMIC mode. The existing `tab.two-column-layout.max-columns` setting and decoration rows still limit the number of real players that fit; no change to this existing limit is included. The normal per-viewer packet diff removes obsolete slots when player counts shrink.
