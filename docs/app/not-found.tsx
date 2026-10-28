import Link from "next/link";

import { Footer } from "@/components/footer";
import { Navbar } from "@/components/navbar";

export default function NotFoundPage() {
  return (
    <main className="relative min-h-screen pb-8">
      <Navbar />

      <section className="section-shell flex min-h-[70vh] items-center py-16">
        <div className="glass-panel max-w-3xl rounded-[36px] px-8 py-12 sm:px-12">
          <span className="inline-flex rounded-full border border-amber-400/20 bg-amber-400/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.28em] text-amber-200">
            Not found
          </span>
          <h1 className="mt-5 text-4xl font-semibold tracking-tight text-white sm:text-5xl">
            That documentation route does not exist.
          </h1>
          <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
            The premium Next.js shell is in place, but this specific route could not be resolved from the
            current OreoEssentials documentation mapping.
          </p>

          <div className="mt-8 flex flex-wrap gap-4">
            <Link href="/docs" className="button-primary">
              Back to docs
            </Link>
            <Link href="/" className="button-secondary">
              Back home
            </Link>
          </div>
        </div>
      </section>

      <Footer />
    </main>
  );
}
