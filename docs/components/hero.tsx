"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";

type HeroStat = {
  label: string;
  value: string;
};

type HeroProps = {
  title: string;
  subtitle: string;
  stats: HeroStat[];
};

export function Hero({ title, subtitle, stats }: HeroProps) {
  return (
    <section className="section-shell relative pt-10 sm:pt-16">
      <div className="grid gap-10 lg:grid-cols-[1.15fr_0.85fr] lg:items-center">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.21, 1, 0.35, 1] }}
          className="relative"
        >
          <span className="inline-flex rounded-full border border-cyan-300/20 bg-cyan-300/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-[0.32em] text-cyan-200/90">
            Premium Minecraft plugin platform
          </span>
          <h1 className="mt-6 max-w-4xl text-5xl font-semibold leading-[1.02] tracking-tight text-white sm:text-6xl lg:text-7xl">
            {title}
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-300 sm:text-xl">{subtitle}</p>

          <div className="mt-8 flex flex-wrap gap-4">
            <Link href="/docs" className="button-primary">
              Explore documentation
            </Link>
            <Link href="/docs/commands-permissions" className="button-secondary">
              View commands
            </Link>
          </div>

          <div className="mt-10 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {stats.map((stat) => (
              <div key={stat.label} className="glass-panel rounded-2xl px-4 py-4">
                <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
                  {stat.label}
                </div>
                <div className="mt-2 text-sm font-medium text-slate-100">{stat.value}</div>
              </div>
            ))}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.94 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.9, delay: 0.15, ease: [0.21, 1, 0.35, 1] }}
          className="relative"
        >
          <div className="glass-panel relative overflow-hidden rounded-[32px] p-6 shadow-premium">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(15,195,255,0.24),transparent_34%),linear-gradient(145deg,rgba(29,78,216,0.18),transparent_70%)]" />
            <div className="relative rounded-[28px] border border-white/10 bg-slate-950/50 p-8">
              <div className="absolute inset-x-12 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/70 to-transparent" />

              <div className="flex items-center justify-between gap-6">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-[0.32em] text-cyan-200/80">
                    OreoEssentials
                  </div>
                  <div className="mt-3 text-2xl font-semibold text-white">Unified plugin stack</div>
                </div>
                <div className="rounded-3xl border border-cyan-300/[0.15] bg-cyan-300/[0.08] p-4 shadow-[0_0_50px_rgba(15,195,255,0.14)]">
                  <Image
                    src="/oreoessentials-logo.png"
                    alt="OreoEssentials logo"
                    width={120}
                    height={120}
                    className="h-20 w-20 object-contain"
                    priority
                  />
                </div>
              </div>

              <div className="mt-8 grid gap-4 sm:grid-cols-2">
                {[
                  "Cross-server messaging",
                  "Integrated economy systems",
                  "Visual modules and holograms",
                  "Modern admin documentation"
                ].map((item) => (
                  <div
                    key={item}
                    className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-4 text-sm text-slate-200"
                  >
                    <span className="mr-3 inline-block h-2 w-2 rounded-full bg-cyan-300 shadow-[0_0_20px_rgba(121,229,255,0.8)]" />
                    {item}
                  </div>
                ))}
              </div>

              <div className="mt-8 rounded-[24px] border border-cyan-300/[0.12] bg-gradient-to-br from-cyan-300/[0.08] via-royal-500/10 to-transparent p-5">
                <div className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">
                  Visual direction
                </div>
                <div className="mt-3 text-sm leading-7 text-slate-300">
                  Deep navy surfaces, royal blue gradients, electric cyan highlights, and glassmorphism
                  shells tuned to the OreoEssentials logo palette.
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
