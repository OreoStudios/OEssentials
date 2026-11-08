import Link from "next/link";

import { docs } from "@/lib/site";

type DocsSidebarProps = {
  currentSlug?: string;
};

export function DocsSidebar({ currentSlug }: DocsSidebarProps) {
  return (
    <aside className="glass-panel rounded-[30px] p-5 lg:sticky lg:top-24">
      <div className="mb-5 border-b border-white/10 pb-5">
        <div className="text-xs font-semibold uppercase tracking-[0.3em] text-cyan-200/80">Navigation</div>
        <h2 className="mt-3 text-2xl font-semibold text-white">Documentation</h2>
        <p className="mt-3 text-sm leading-7 text-slate-300">
          Every module, player system, economy surface, and platform area in one structured portal.
        </p>
      </div>

      <div className="max-h-[70vh] space-y-2 overflow-y-auto pr-1">
        {docs.map((doc) => {
          const isActive = currentSlug === doc.slug;

          return (
            <Link
              key={doc.slug}
              href={`/docs/${doc.slug}`}
              className={`block rounded-2xl border px-4 py-3 transition ${
                isActive
                  ? "border-cyan-300/30 bg-cyan-300/10 shadow-[0_0_0_1px_rgba(121,229,255,0.12)]"
                  : "border-white/10 bg-white/[0.03] hover:border-cyan-300/[0.18] hover:bg-cyan-300/[0.06]"
              }`}
            >
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm font-semibold text-white">{doc.title}</span>
                <span
                  className={`rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.18em] ${
                    doc.status === "live"
                      ? "bg-emerald-400/10 text-emerald-200"
                      : "bg-amber-400/10 text-amber-200"
                  }`}
                >
                  {doc.status === "live" ? "Live" : "Soon"}
                </span>
              </div>
              <div className="mt-2 font-mono text-[11px] text-slate-500">{doc.sourceFile}</div>
            </Link>
          );
        })}
      </div>
    </aside>
  );
}
