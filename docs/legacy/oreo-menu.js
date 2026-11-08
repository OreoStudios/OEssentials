(function () {
  const menuItems = [
    { href: "index.html", title: "Introduction", file: "index.html", desc: "Project overview, setup flow, architecture, and documentation hub." },
    { href: "commands.html", title: "Commands / Permissions", file: "commands.html", desc: "Full command, alias, and permission reference." },
    { href: "afk.html", title: "AFK", file: "afk.html", desc: "AFK detection, timers, status changes, and behavior." },
    { href: "auctionhouse.html", title: "Auction House", file: "auctionhouse.html", desc: "Listings, fees, expiries, browsing, and moderation." },
    { href: "chat-format.html", title: "Chat & Messaging", file: "chat-format.html", desc: "Channels, formatting, private messages, and chat systems." },
    { href: "commands-module.html", title: "Commands Module", file: "commands-module.html", desc: "Command toggles, aliases, and command module internals." },
    { href: "custom-currencies.html", title: "Custom Currencies", file: "custom-currencies.html", desc: "Alternative currencies, balances, and custom economy flows." },
    { href: "custom-nameplates.html", title: "Custom Nameplates", file: "custom-nameplates.html", desc: "Player nameplate customization and display behavior." },
    { href: "daily-rewards.html", title: "Daily Rewards", file: "daily-rewards.html", desc: "Reward schedules, claiming rules, and cooldown behavior." },
    { href: "data.html", title: "Data", file: "data.html", desc: "Reserved page for storage structure and persistent data docs." },
    { href: "enderchest.html", title: "Ender Chests", file: "enderchest.html", desc: "Tiered ender chests, permissions, and related storage." },
    { href: "lang.html", title: "Lang", file: "lang.html", desc: "Reserved page for language keys and translation docs." },
    { href: "lib.html", title: "Lib", file: "lib.html", desc: "Reserved page for bundled libraries and dependency notes." },
    { href: "logs.html", title: "Logs", file: "logs.html", desc: "Reserved page for logging formats and audit output." },
    { href: "holograms.html", title: "Holograms", file: "holograms.html", desc: "Hologram creation, displays, visibility, and commands." },
    { href: "orders.html", title: "Orders", file: "orders.html", desc: "Market orders, order books, and economy trading flows." },
    { href: "players.html", title: "Players", file: "players.html", desc: "Reserved page for player-facing systems and player data docs." },
    { href: "playervaults.html", title: "Player Vaults", file: "playervaults.html", desc: "Virtual vault storage, limits, and access controls." },
    { href: "playerwarps.html", title: "Player Warps", file: "playerwarps.html", desc: "Player-owned public warps and warp management." },
    { href: "playtime-rewards.html", title: "Playtime Rewards", file: "playtime-rewards.html", desc: "Timed rewards, milestones, and reward state tracking." },
    { href: "portals.html", title: "Portals", file: "portals.html", desc: "Portal creation, linking, actions, and travel behavior." },
    { href: "tab-bossbar.html", title: "Scoreboard, Tab & Bossbar", file: "tab-bossbar.html", desc: "Scoreboard, player list, and boss bar configuration." },
    { href: "server.html", title: "Server", file: "server.html", desc: "Core server settings, MOTD, maintenance, rules, and utilities." },
    { href: "shop.html", title: "Shop", file: "shop.html", desc: "GUI shop setup, pricing, stock, and category design." },
    { href: "trade.html", title: "Trades", file: "trade.html", desc: "Player-to-player trading, trade safety, and sync behavior." },
    { href: "vaults.html", title: "Vaults", file: "vaults.html", desc: "Reserved page for shared vault systems and vault notes." }
  ];

  const currentFile = (window.location.pathname.split("/").pop() || "index.html").toLowerCase();

  document.body.classList.add("has-site-menu");

  function buildDocsSection() {
    const section = document.createElement("section");
    section.className = "docs-sidebar-card";
    section.innerHTML = [
      '<div class="docs-sidebar-header">',
      '  <div class="docs-sidebar-brand">',
      '    <img src="oreoessentials-logo.png" alt="OreoEssentials logo">',
      '    <div class="docs-sidebar-copy">',
      '      <strong>All Documentation</strong>',
      '      <span>Clean page list</span>',
      "    </div>",
      "  </div>",
      "</div>",
      '<div class="docs-sidebar-list" aria-label="All documentation pages"></div>'
    ].join("");

    const list = section.querySelector(".docs-sidebar-list");
    menuItems.forEach((item) => {
      const link = document.createElement("a");
      link.className = "docs-sidebar-link";
      link.href = item.href;
      link.innerHTML = [
        '<span class="docs-sidebar-link-title">',
        item.title,
        "</span>",
        '<span class="docs-sidebar-link-file">',
        item.file,
        "</span>"
      ].join("");

      if (item.href.toLowerCase() === currentFile) {
        link.classList.add("current");
        link.setAttribute("aria-current", "page");
        document.title = "OreoEssentials | " + item.title;
      }

      list.appendChild(link);
    });

    return section;
  }

  const existingSidebar = document.querySelector(".sidebar");
  if (existingSidebar) {
    const docsSection = buildDocsSection();
    const brand = existingSidebar.querySelector(".brand");
    if (brand && brand.parentElement === existingSidebar) {
      brand.insertAdjacentElement("afterend", docsSection);
    } else {
      existingSidebar.insertBefore(docsSection, existingSidebar.firstChild);
    }
  } else {
    const originalNodes = Array.from(document.body.childNodes);
    const shell = document.createElement("div");
    shell.className = "docs-page-shell";

    const aside = document.createElement("aside");
    aside.className = "docs-page-sidebar";
    aside.appendChild(buildDocsSection());

    const content = document.createElement("div");
    content.className = "docs-page-content";
    originalNodes.forEach((node) => {
      content.appendChild(node);
    });

    shell.appendChild(aside);
    shell.appendChild(content);
    document.body.appendChild(shell);
  }

  document.querySelectorAll("nav").forEach((existingNav) => {
    if (existingNav.classList.contains("sidebar") || existingNav.closest(".sidebar")) {
      return;
    }
    existingNav.classList.add("page-nav");
  });

  const docsGrid = document.querySelector("#docs .doc-links");
  if (currentFile === "index.html" && docsGrid) {
    docsGrid.classList.add("menu-grid");
    docsGrid.innerHTML = menuItems
      .map(function (item) {
        return [
          '<a class="doc-link menu-link" href="',
          item.href,
          '">',
          '<span class="dl-title">',
          item.title,
          "</span>",
          '<span class="dl-desc">',
          item.file,
          "</span>",
          "</a>"
        ].join("");
      })
      .join("");
  }
})();
