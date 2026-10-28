import Link from "next/link";

export function Footer() {
  return (
    <footer className="section-shell pb-10 pt-4">
      <div className="flex flex-col gap-5 border-t border-white/10 py-6 text-sm text-slate-400 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="font-semibold uppercase tracking-[0.24em] text-slate-300">OreoEssentials</div>
          <div className="mt-1">Premium documentation frontend for Paper / Folia plugin systems.</div>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <Link href="/" className="transition hover:text-cyan-200">
            Home
          </Link>
          <Link href="/docs" className="transition hover:text-cyan-200">
            Documentation
          </Link>
          <Link href="/docs/commands-permissions" className="transition hover:text-cyan-200">
            Commands
          </Link>
        </div>
      </div>
    </footer>
  );
}
