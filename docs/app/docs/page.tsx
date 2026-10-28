import type { Metadata } from "next";

import { CTASection } from "@/components/cta-section";
import { DocsPreviewCard } from "@/components/docs-preview-card";
import { FadeInSection } from "@/components/fade-in-section";
import { Footer } from "@/components/footer";
import { Navbar } from "@/components/navbar";
import { SectionHeading } from "@/components/section-heading";
import { docs } from "@/lib/site";

export const metadata: Metadata = {
  title: "Documentation",
  description: "Browse every OreoEssentials documentation page in the new premium frontend."
};

export default function DocsIndexPage() {
  return (
    <main className="relative pb-8">
      <Navbar />

      <section className="section-shell pt-12 sm:pt-16">
        <FadeInSection className="glass-panel rounded-[36px] px-6 py-10 sm:px-10 sm:py-12">
          <div className="max-w-4xl">
            <span className="inline-flex rounded-full border border-cyan-300/20 bg-cyan-300/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.3em] text-cyan-200/90">
              Documentation control center
            </span>
            <h1 className="mt-5 text-4xl font-semibold tracking-tight text-white sm:text-5xl">
              A premium docs portal for every OreoEssentials module.
            </h1>
            <p className="mt-5 max-w-3xl text-base leading-8 text-slate-300 sm:text-lg">
              Every current HTML page is now represented as a structured documentation route. Use this
              portal as the main entry point for setup, commands, economy systems, player features,
              visuals, storage, rewards, and platform internals.
            </p>
          </div>

          <div className="mt-8 flex flex-wrap gap-3 text-sm text-slate-300">
            <div className="glass-panel rounded-2xl px-4 py-3">{docs.length} pages mapped into Next.js</div>
            <div className="glass-panel rounded-2xl px-4 py-3">Tailwind-powered premium shell</div>
            <div className="glass-panel rounded-2xl px-4 py-3">Framer Motion interactions</div>
          </div>
        </FadeInSection>
      </section>

      <FadeInSection className="section-shell pt-20">
        <SectionHeading
          eyebrow="Full catalog"
          title="All documentation pages"
          description="Readable naming, premium layout, and future-friendly routes for every current source file."
        />

        <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {docs.map((doc) => (
            <DocsPreviewCard key={doc.slug} doc={doc} />
          ))}
        </div>
      </FadeInSection>

      <CTASection />
      <Footer />
    </main>
  );
}
