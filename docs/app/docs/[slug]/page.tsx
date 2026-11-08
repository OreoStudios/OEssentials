import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { CTASection } from "@/components/cta-section";
import { DocsSidebar } from "@/components/docs-sidebar";
import { FadeInSection } from "@/components/fade-in-section";
import { Footer } from "@/components/footer";
import { Navbar } from "@/components/navbar";
import { getLegacyDocHtml } from "@/lib/legacy-docs";
import { docs, getDocBySlug, getDocIndex } from "@/lib/site";

type DocPageProps = {
  params: Promise<{
    slug: string;
  }>;
};

export function generateStaticParams() {
  return docs.map((doc) => ({ slug: doc.slug }));
}

export async function generateMetadata({ params }: DocPageProps): Promise<Metadata> {
  const resolved = await params;
  const doc = getDocBySlug(resolved.slug);

  if (!doc) {
    return {
      title: "Document not found"
    };
  }

  return {
    title: doc.title,
    description: doc.description
  };
}

export default async function DocDetailPage({ params }: DocPageProps) {
  const resolved = await params;
  const doc = getDocBySlug(resolved.slug);

  if (!doc) {
    notFound();
  }

  const docIndex = getDocIndex(doc.slug);
  const previousDoc = docIndex > 0 ? docs[docIndex - 1] : null;
  const nextDoc = docIndex < docs.length - 1 ? docs[docIndex + 1] : null;
  const legacyHtml = doc.status === "live" ? getLegacyDocHtml(doc.sourceFile) : null;

  return (
    <main className="relative pb-8">
      <Navbar />

      <section className="section-shell pt-10 sm:pt-14">
        <div className="grid gap-8 xl:grid-cols-[320px_minmax(0,1fr)]">
          <FadeInSection className="xl:order-1">
            <DocsSidebar currentSlug={doc.slug} />
          </FadeInSection>

          <div className="space-y-8">
            <FadeInSection>
              <div className="glass-panel rounded-[34px] px-6 py-8 sm:px-10 sm:py-10">
                <div className="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
                  <Link href="/docs" className="transition hover:text-cyan-200">
                    Documentation
                  </Link>
                  <span>/</span>
                  <span className="text-cyan-200">{doc.category}</span>
                </div>

                <h1 className="mt-5 text-4xl font-semibold tracking-tight text-white sm:text-5xl">
                  {doc.title}
                </h1>
                <p className="mt-5 max-w-3xl text-base leading-8 text-slate-300 sm:text-lg">
                  {doc.description}
                </p>

                <div className="mt-8 flex flex-wrap gap-3">
                  <span className="glass-panel rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-slate-300">
                    Source: {doc.sourceFile}
                  </span>
                  <span
                    className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] ${
                      doc.status === "live"
                        ? "border border-emerald-400/20 bg-emerald-400/10 text-emerald-200"
                        : "border border-amber-400/20 bg-amber-400/10 text-amber-200"
                    }`}
                  >
                    {doc.status === "live" ? "Live page" : "Coming soon"}
                  </span>
                </div>
              </div>
            </FadeInSection>

            <FadeInSection delay={0.05}>
              {legacyHtml ? (
                <article className="legacy-doc glass-panel rounded-[34px] p-6 sm:p-8 lg:p-10">
                  <div dangerouslySetInnerHTML={{ __html: legacyHtml }} />
                </article>
              ) : (
                <section className="glass-panel rounded-[34px] p-8 sm:p-10">
                  <div className="max-w-3xl">
                    <span className="inline-flex rounded-full border border-amber-400/20 bg-amber-400/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.28em] text-amber-200">
                      Coming soon
                    </span>
                    <h2 className="mt-5 text-3xl font-semibold text-white">This page is reserved in the new portal.</h2>
                    <p className="mt-4 text-base leading-8 text-slate-300">
                      The route and premium layout are already in place. The original placeholder content
                      has been preserved in the project, and this page is ready for a deeper rewrite when
                      you want to expand the documentation.
                    </p>
                  </div>
                </section>
              )}
            </FadeInSection>

            <FadeInSection delay={0.1}>
              <div className="grid gap-4 md:grid-cols-2">
                {previousDoc ? (
                  <Link href={`/docs/${previousDoc.slug}`} className="glass-panel rounded-[28px] p-5 transition hover:-translate-y-1">
                    <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
                      Previous
                    </div>
                    <div className="mt-3 text-xl font-semibold text-white">{previousDoc.title}</div>
                    <div className="mt-2 text-sm text-slate-300">{previousDoc.sourceFile}</div>
                  </Link>
                ) : (
                  <div className="glass-panel rounded-[28px] p-5 opacity-60">
                    <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
                      Previous
                    </div>
                    <div className="mt-3 text-xl font-semibold text-white">Start of catalog</div>
                    <div className="mt-2 text-sm text-slate-300">No earlier page in the route order.</div>
                  </div>
                )}

                {nextDoc ? (
                  <Link href={`/docs/${nextDoc.slug}`} className="glass-panel rounded-[28px] p-5 transition hover:-translate-y-1">
                    <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Next</div>
                    <div className="mt-3 text-xl font-semibold text-white">{nextDoc.title}</div>
                    <div className="mt-2 text-sm text-slate-300">{nextDoc.sourceFile}</div>
                  </Link>
                ) : (
                  <div className="glass-panel rounded-[28px] p-5 opacity-60">
                    <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Next</div>
                    <div className="mt-3 text-xl font-semibold text-white">End of catalog</div>
                    <div className="mt-2 text-sm text-slate-300">No later page in the route order.</div>
                  </div>
                )}
              </div>
            </FadeInSection>
          </div>
        </div>
      </section>

      <CTASection />
      <Footer />
    </main>
  );
}
