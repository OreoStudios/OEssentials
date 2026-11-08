export type DocStatus = "live" | "coming-soon";

export type DocItem = {
  slug: string;
  title: string;
  description: string;
  sourceFile: string;
  category: string;
  status: DocStatus;
};

export const docs: DocItem[] = [
  {
    slug: "introduction",
    title: "Introduction",
    description: "Project overview, setup flow, architecture, and the full documentation hub.",
    sourceFile: "index.html",
    category: "Core",
    status: "live"
  },
  {
    slug: "commands-permissions",
    title: "Commands / Permissions",
    description: "Every command, alias, permission node, and default access rule.",
    sourceFile: "commands.html",
    category: "Core",
    status: "live"
  },
  {
    slug: "afk",
    title: "AFK",
    description: "AFK detection, timers, state changes, and player-facing behavior.",
    sourceFile: "afk.html",
    category: "Player Systems",
    status: "live"
  },
  {
    slug: "auction-house",
    title: "Auction House",
    description: "Listings, fees, expiries, browsing, and auction moderation flows.",
    sourceFile: "auctionhouse.html",
    category: "Economy",
    status: "live"
  },
  {
    slug: "chat-messaging",
    title: "Chat & Messaging",
    description: "Channels, formatting, private messages, and network-aware communication.",
    sourceFile: "chat-format.html",
    category: "Communication",
    status: "live"
  },
  {
    slug: "commands-module",
    title: "Commands Module",
    description: "Alias handling, toggles, and the command module control surface.",
    sourceFile: "commands-module.html",
    category: "Core",
    status: "live"
  },
  {
    slug: "custom-currencies",
    title: "Custom Currencies",
    description: "Alternative currencies, balances, transfers, leaderboards, and storage.",
    sourceFile: "custom-currencies.html",
    category: "Economy",
    status: "live"
  },
  {
    slug: "custom-nameplates",
    title: "Custom Nameplates",
    description: "Player nameplate customization and display behavior.",
    sourceFile: "custom-nameplates.html",
    category: "Player Systems",
    status: "live"
  },
  {
    slug: "daily-rewards",
    title: "Daily Rewards",
    description: "Reward schedules, streak rules, GUIs, and claim behavior.",
    sourceFile: "daily-rewards.html",
    category: "Rewards",
    status: "live"
  },
  {
    slug: "data",
    title: "Data",
    description: "Reserved page for storage structure and persistent data documentation.",
    sourceFile: "data.html",
    category: "Platform",
    status: "coming-soon"
  },
  {
    slug: "ender-chests",
    title: "Ender Chests",
    description: "Tiered ender chests, permissions, virtual storage, and related tooling.",
    sourceFile: "enderchest.html",
    category: "Storage",
    status: "live"
  },
  {
    slug: "lang",
    title: "Lang",
    description: "Reserved page for language keys, translations, and message customization.",
    sourceFile: "lang.html",
    category: "Platform",
    status: "coming-soon"
  },
  {
    slug: "lib",
    title: "Lib",
    description: "Reserved page for bundled libraries and dependency notes.",
    sourceFile: "lib.html",
    category: "Platform",
    status: "coming-soon"
  },
  {
    slug: "logs",
    title: "Logs",
    description: "Reserved page for logs, audit output, and operations tracing.",
    sourceFile: "logs.html",
    category: "Platform",
    status: "coming-soon"
  },
  {
    slug: "holograms",
    title: "Holograms",
    description: "Hologram creation, display entities, visibility rules, and commands.",
    sourceFile: "holograms.html",
    category: "Visual Systems",
    status: "live"
  },
  {
    slug: "orders",
    title: "Orders",
    description: "Market orders, order books, and economy trading flows.",
    sourceFile: "orders.html",
    category: "Economy",
    status: "live"
  },
  {
    slug: "players",
    title: "Players",
    description: "Reserved page for player-facing systems and player data docs.",
    sourceFile: "players.html",
    category: "Player Systems",
    status: "coming-soon"
  },
  {
    slug: "player-vaults",
    title: "Player Vaults",
    description: "Virtual vault storage, access limits, and player inventory expansion.",
    sourceFile: "playervaults.html",
    category: "Storage",
    status: "live"
  },
  {
    slug: "player-warps",
    title: "Player Warps",
    description: "Player-owned public warps and warp discovery / management flows.",
    sourceFile: "playerwarps.html",
    category: "Travel",
    status: "live"
  },
  {
    slug: "playtime-rewards",
    title: "Playtime Rewards",
    description: "Timed rewards, milestones, and long-session progression rewards.",
    sourceFile: "playtime-rewards.html",
    category: "Rewards",
    status: "live"
  },
  {
    slug: "portals",
    title: "Portals",
    description: "Portal creation, linking, actions, and travel behavior.",
    sourceFile: "portals.html",
    category: "Travel",
    status: "live"
  },
  {
    slug: "scoreboard-tab-bossbar",
    title: "Scoreboard, Tab & Bossbar",
    description: "Scoreboard, player list, and boss bar configuration.",
    sourceFile: "tab-bossbar.html",
    category: "Visual Systems",
    status: "live"
  },
  {
    slug: "server",
    title: "Server",
    description: "Core server settings, MOTD, maintenance, rules, and utilities.",
    sourceFile: "server.html",
    category: "Core",
    status: "live"
  },
  {
    slug: "shop",
    title: "Shop",
    description: "GUI shop setup, pricing, categories, stock, and economy integration.",
    sourceFile: "shop.html",
    category: "Economy",
    status: "live"
  },
  {
    slug: "trades",
    title: "Trades",
    description: "Player-to-player trading, safety rules, and cross-server handling.",
    sourceFile: "trade.html",
    category: "Economy",
    status: "live"
  },
  {
    slug: "vaults",
    title: "Vaults",
    description: "Reserved page for shared vault systems and vault-specific notes.",
    sourceFile: "vaults.html",
    category: "Storage",
    status: "coming-soon"
  }
];

export const navItems = [
  { label: "Home", href: "/" },
  { label: "Documentation", href: "/docs" },
  { label: "Commands", href: "/docs/commands-permissions" },
  { label: "Shop", href: "/docs/shop" },
  { label: "Auction House", href: "/docs/auction-house" }
];

export const heroStats = [
  { label: "Platform", value: "Paper / Folia 1.21+" },
  { label: "Runtime", value: "Java 21+" },
  { label: "Modules", value: "25+ integrated systems" },
  { label: "Networking", value: "Cross-server architecture" }
];

export const landingFeatures = [
  {
    title: "Cross-server everything",
    description:
      "Teleportation, messaging, trades, inventories, and player sync behave like a unified platform instead of disconnected plugins."
  },
  {
    title: "Economy stack built in",
    description:
      "Vault support, custom currencies, shop, auction house, and market orders ship as one coherent ecosystem."
  },
  {
    title: "High-control player systems",
    description:
      "Homes, warps, vaults, ender chests, playtime rewards, daily rewards, AFK, and nameplates stay deeply configurable."
  },
  {
    title: "Modern visual modules",
    description:
      "Holograms, scoreboard, tab, bossbars, and polished in-game interfaces create a premium server experience."
  },
  {
    title: "Admin and moderation tooling",
    description:
      "Server utilities, maintenance, commands control, logs, and moderation-oriented workflows are documented in one place."
  },
  {
    title: "Built like software, not a plugin pile",
    description:
      "One product, one identity, one documentation surface, with every subsystem designed to work together."
  }
];

export const productPillars = [
  {
    title: "Integrated platform design",
    description:
      "OreoEssentials replaces stacked one-off plugins with a single system that feels cohesive across setup, gameplay, and administration."
  },
  {
    title: "Premium admin experience",
    description:
      "Documentation, commands, modules, and workflows are structured like a modern SaaS product instead of a fragmented wiki."
  },
  {
    title: "Network-aware architecture",
    description:
      "The product direction is intentionally built for Paper and Folia environments where multi-server behavior matters."
  }
];

export const setupHighlights = [
  "Paper / Folia 1.21+ focused feature set",
  "RabbitMQ / Redis friendly multi-server direction",
  "Single docs portal for commands, economy, storage, visual systems, and rewards",
  "SEO-friendly documentation routes generated from your existing source HTML"
];

export const featuredDocs = docs.filter((doc) =>
  [
    "introduction",
    "commands-permissions",
    "auction-house",
    "custom-currencies",
    "holograms",
    "shop"
  ].includes(doc.slug)
);

export function getDocBySlug(slug: string) {
  return docs.find((doc) => doc.slug === slug);
}

export function getDocIndex(slug: string) {
  return docs.findIndex((doc) => doc.slug === slug);
}
