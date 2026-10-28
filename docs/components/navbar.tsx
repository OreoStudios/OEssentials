"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";

import { navItems } from "@/lib/site";

export function Navbar() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-slate-950/65 backdrop-blur-xl">
      <div className="section-shell">
        <div className="flex items-center justify-between gap-4 py-4">
          <Link href="/" className="flex items-center gap-4">
            <div className="rounded-2xl border border-cyan-300/[0.15] bg-cyan-300/[0.08] p-2 shadow-[0_0_40px_rgba(15,195,255,0.12)]">
              <Image
                src="/oreoessentials-logo.png"
                alt="OreoEssentials logo"
                width={42}
                height={42}
                className="h-10 w-10 object-contain"
                priority
              />
            </div>
            <div>
              <div className="text-sm font-semibold uppercase tracking-[0.3em] text-white">OreoEssentials</div>
              <div className="text-xs uppercase tracking-[0.22em] text-slate-400">Premium docs frontend</div>
            </div>
          </Link>

          <nav className="hidden items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-2 lg:flex">
            {navItems.map((item) => {
              const active = pathname === item.href || (item.href !== "/" && pathname.startsWith(item.href));

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                    active
                      ? "bg-cyan-300/[0.12] text-cyan-100 shadow-[0_0_0_1px_rgba(121,229,255,0.12)]"
                      : "text-slate-300 hover:bg-white/[0.05] hover:text-white"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <div className="hidden lg:block">
            <Link href="/docs" className="button-primary">
              Open docs
            </Link>
          </div>

          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.04] text-white transition hover:border-cyan-300/20 hover:bg-cyan-300/10 lg:hidden"
            aria-label="Toggle navigation"
            aria-expanded={open}
          >
            <span className="flex flex-col gap-1.5">
              <span className="block h-0.5 w-5 rounded-full bg-current" />
              <span className="block h-0.5 w-5 rounded-full bg-current" />
              <span className="block h-0.5 w-5 rounded-full bg-current" />
            </span>
          </button>
        </div>

        <AnimatePresence>
          {open ? (
            <motion.div
              initial={{ opacity: 0, y: -12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.2 }}
              className="pb-4 lg:hidden"
            >
              <div className="glass-panel rounded-[28px] p-3">
                <div className="grid gap-2">
                  {navItems.map((item) => (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setOpen(false)}
                      className={`rounded-2xl px-4 py-3 text-sm font-medium transition ${
                        pathname === item.href || (item.href !== "/" && pathname.startsWith(item.href))
                          ? "bg-cyan-300/[0.12] text-cyan-100"
                          : "text-slate-300 hover:bg-white/[0.05] hover:text-white"
                      }`}
                    >
                      {item.label}
                    </Link>
                  ))}
                </div>
              </div>
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </header>
  );
}
