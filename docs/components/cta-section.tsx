"use client";

import Link from "next/link";
import { motion } from "framer-motion";

export function CTASection() {
  return (
    <motion.section
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={{ duration: 0.7, ease: [0.21, 1, 0.35, 1] }}
      className="section-shell pb-20 pt-8 sm:pb-24"
    >
      <div className="glass-panel relative overflow-hidden rounded-[36px] px-6 py-10 sm:px-10 sm:py-12 lg:px-14">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(50,213,255,0.16),transparent_30%),linear-gradient(135deg,rgba(36,89,223,0.14),transparent_60%)]" />
        <div className="relative flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full border border-cyan-300/20 bg-cyan-300/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.28em] text-cyan-200/90">
              Build-ready frontend
            </span>
            <h2 className="mt-5 text-3xl font-semibold tracking-tight text-white sm:text-4xl">
              Ready for a premium Next.js documentation experience.
            </h2>
            <p className="mt-4 text-base leading-8 text-slate-300 sm:text-lg">
              The new frontend is structured for landing pages, module docs, future search, and deeper
              product marketing without losing your original documentation source content.
            </p>
          </div>

          <div className="flex flex-wrap gap-4">
            <Link href="/docs" className="button-primary">
              Browse all docs
            </Link>
            <Link href="/docs/introduction" className="button-secondary">
              Open introduction
            </Link>
          </div>
        </div>
      </div>
    </motion.section>
  );
}
