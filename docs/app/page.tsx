import Link from "next/link";

import { CTASection } from "@/components/cta-section";
import { DocsPreviewCard } from "@/components/docs-preview-card";
import { FadeInSection } from "@/components/fade-in-section";
import { FeatureCard } from "@/components/feature-card";
import { Footer } from "@/components/footer";
import { Hero } from "@/components/hero";
import { Navbar } from "@/components/navbar";
import { SectionHeading } from "@/components/section-heading";
import {
  docs,
  featuredDocs,
  heroStats,
  landingFeatures,
  productPillars,
  setupHighlights
} from "@/lib/site";

export default function HomePage() {
  return (
    <main className="relative pb-8">
      <Navbar />

      <Hero
        title="The premium frontend layer for a serious Minecraft plugin ecosystem."
        subtitle="OreoEssentials now ships with a high-end documentation experience built in Next.js, React, Tailwind CSS, and Framer Motion - tuned to the deep navy, royal blue, and electric cyan identity of your logo."
        stats={heroStats}
      />

      <FadeInSection className="section-shell pt-20">
        <SectionHeading
          eyebrow="Why it feels different"
          title="A premium SaaS-style surface for a plugin platform that deserves more than flat docs."
          description="The new frontend keeps the existing source content intact while upgrading the presentation into a polished product website: stronger hierarchy, cleaner navigation, smoother motion, and a sharper high-end visual system."
        />

        <div className="mt-10 grid gap-6 lg:grid-cols-3">
          {productPillars.map((pillar) => (
            <div key={pillar.title} className="glass-panel rounded-[28px] p-6">
              <div className="mb-5 inline-flex rounded-2xl border border-cyan-300/[0.15] bg-cyan-300/10 px-3 py-2 text-xs font-semibold uppercase tracking-[0.28em] text-cyan-200/90">
                System pillar
              </div>
              <h3 className="text-xl font-semibold text-white">{pillar.title}</h3>
              <p className="mt-3 text-sm leading-7 text-slate-300">{pillar.description}</p>
            </div>
          ))}
        </div>
      </FadeInSection>

      <FadeInSection className="section-shell pt-24">
        <SectionHeading
          eyebrow="Core value"
          title="Every important system is now framed like a product, not a loose collection of HTML pages."
          description="The content stays complete, but the experience is upgraded for real users: server owners, staff, builders, and technical admins who need clarity, depth, and confidence."
        />

        <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {landingFeatures.map((feature) => (
            <FeatureCard key={feature.title} title={feature.title} description={feature.description} />
          ))}
        </div>
      </FadeInSection>

      <FadeInSection className="section-shell pt-24">
        <div className="grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
          <div>
            <SectionHeading
              eyebrow="Migration strategy"
              title="Your existing documentation stays intact."
              description="Instead of throwing away the current docs, the new app wraps them in a production-grade interface and turns the current HTML files into structured documentation routes."
            />
          </div>

          <div className="glass-panel rounded-[32px] p-6 sm:p-8">
            <div className="grid gap-4">
              {setupHighlights.map((highlight, index) => (
                <div
                  key={highlight}
                  className="rounded-[22px] border border-white/10 bg-white/[0.03] px-5 py-4 text-sm text-slate-200"
                >
                  <div className="mb-2 text-xs font-semibold uppercase tracking-[0.26em] text-cyan-200/80">
                    0{index + 1}
                  </div>
                  <div className="leading-7">{highlight}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </FadeInSection>

      <FadeInSection className="section-shell pt-24">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <SectionHeading
            eyebrow="Documentation"
            title="Featured pages"
            description="Launch into the most important modules first, then branch out into the full portal."
          />

          <div className="sm:pb-2">
            <Link href="/docs" className="button-secondary">
              View all {docs.length} documentation pages
            </Link>
          </div>
        </div>

        <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {featuredDocs.map((doc) => (
            <DocsPreviewCard key={doc.slug} doc={doc} />
          ))}
        </div>
      </FadeInSection>

      <CTASection />
      <Footer />
    </main>
  );
}
