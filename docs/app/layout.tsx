import type { Metadata } from "next";
import type { ReactNode } from "react";

import { AnimatedBackground } from "@/components/animated-background";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "OreoEssentials | Premium Documentation Portal",
    template: "%s | OreoEssentials"
  },
  description:
    "Premium Next.js frontend for OreoEssentials documentation, commands, modules, economy systems, and plugin platform content.",
  keywords: [
    "OreoEssentials",
    "Minecraft plugin",
    "Paper",
    "Folia",
    "plugin documentation",
    "Next.js frontend"
  ]
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" className="scroll-smooth">
      <body>
        <AnimatedBackground />
        {children}
      </body>
    </html>
  );
}
