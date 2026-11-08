"use client";

import { motion } from "framer-motion";

type FeatureCardProps = {
  title: string;
  description: string;
};

export function FeatureCard({ title, description }: FeatureCardProps) {
  return (
    <motion.div
      whileHover={{ y: -6, scale: 1.01 }}
      transition={{ type: "spring", stiffness: 260, damping: 20 }}
      className="glass-panel group relative overflow-hidden rounded-[28px] p-6"
    >
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-cyan-300/60 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
      <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-cyan-300/[0.15] bg-cyan-300/10 text-cyan-200 shadow-[0_0_32px_rgba(15,195,255,0.12)]">
        <span className="h-2.5 w-2.5 rounded-full bg-cyan-300" />
      </div>
      <h3 className="text-xl font-semibold text-white">{title}</h3>
      <p className="mt-3 text-sm leading-7 text-slate-300">{description}</p>
    </motion.div>
  );
}
