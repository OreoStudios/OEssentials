"use client";

import Link from "next/link";
import { motion } from "framer-motion";

import type { DocItem } from "@/lib/site";

type DocsPreviewCardProps = {
  doc: DocItem;
};

export function DocsPreviewCard({ doc }: DocsPreviewCardProps) {
  return (
    <motion.div whileHover={{ y: -5 }} transition={{ type: "spring", stiffness: 260, damping: 20 }}>
      <Link href={`/docs/${doc.slug}`} className="glass-panel group block rounded-[28px] p-6">
        <div className="flex items-start justify-between gap-3">
          <span className="inline-flex rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-cyan-200/90">
            {doc.category}
          </span>
          <span
            className={`inline-flex rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] ${
              doc.status === "live"
                ? "border border-emerald-400/20 bg-emerald-400/10 text-emerald-200"
                : "border border-amber-400/20 bg-amber-400/10 text-amber-200"
            }`}
          >
            {doc.status === "live" ? "Live" : "Soon"}
          </span>
        </div>
        <h3 className="mt-5 text-xl font-semibold text-white transition-colors group-hover:text-cyan-200">
          {doc.title}
        </h3>
        <p className="mt-3 text-sm leading-7 text-slate-300">{doc.description}</p>
        <div className="mt-6 flex items-center justify-between text-sm text-slate-400">
          <span className="font-mono text-xs text-slate-500">{doc.sourceFile}</span>
          <span className="text-cyan-200 transition-transform group-hover:translate-x-1">Open</span>
        </div>
      </Link>
    </motion.div>
  );
}
