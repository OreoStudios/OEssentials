"use client";

import type { PropsWithChildren } from "react";
import { motion } from "framer-motion";

type FadeInSectionProps = PropsWithChildren<{
  className?: string;
  delay?: number;
}>;

export function FadeInSection({ children, className, delay = 0 }: FadeInSectionProps) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 28 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.2 }}
      transition={{ duration: 0.7, delay, ease: [0.21, 1, 0.35, 1] }}
      className={className}
    >
      {children}
    </motion.section>
  );
}
