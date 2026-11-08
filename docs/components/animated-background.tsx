"use client";

import { motion } from "framer-motion";

const orbs = [
  {
    className: "left-[-8rem] top-[-6rem] h-72 w-72 bg-cyan-400/[0.18]",
    animate: { x: [0, 30, -10, 0], y: [0, 24, -16, 0], scale: [1, 1.06, 0.98, 1] }
  },
  {
    className: "right-[-7rem] top-40 h-80 w-80 bg-blue-500/16",
    animate: { x: [0, -28, 14, 0], y: [0, 18, -20, 0], scale: [1, 0.96, 1.08, 1] }
  },
  {
    className: "bottom-[-8rem] left-1/3 h-96 w-96 bg-sky-500/10",
    animate: { x: [0, 20, -26, 0], y: [0, -22, 12, 0], scale: [1, 1.04, 0.94, 1] }
  }
];

export function AnimatedBackground() {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(15,195,255,0.08),transparent_36%),linear-gradient(180deg,#040914_0%,#08101d_42%,#050b15_100%)]" />
      <div className="absolute inset-0 bg-hero-grid bg-[size:88px_88px] opacity-[0.06]" />
      <div className="absolute inset-0 bg-[linear-gradient(180deg,transparent,rgba(4,8,18,0.28)_30%,rgba(4,8,18,0.72)_100%)]" />

      {orbs.map((orb) => (
        <motion.div
          key={orb.className}
          animate={orb.animate}
          transition={{ duration: 14, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut" }}
          className={`absolute rounded-full blur-3xl ${orb.className}`}
        />
      ))}
    </div>
  );
}
