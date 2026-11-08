import fs from "node:fs";
import path from "node:path";

import { docs } from "@/lib/site";

type ExtractedElement = {
  outer: string;
  inner: string;
  start: number;
  end: number;
};

function readLegacyFile(sourceFile: string) {
  const legacyPath = path.join(process.cwd(), "legacy", sourceFile);
  const fallbackPath = path.join(process.cwd(), sourceFile);
  const absolutePath = fs.existsSync(legacyPath) ? legacyPath : fallbackPath;
  return fs.readFileSync(absolutePath, "utf8");
}

function extractBody(html: string) {
  const match = html.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
  return match ? match[1] : html;
}

function findStartTagIndex(html: string, tag: string, className?: string) {
  if (!className) {
    const plain = new RegExp(`<${tag}\\b[^>]*>`, "i");
    const match = plain.exec(html);
    return match?.index ?? -1;
  }

  const withClass = new RegExp(
    `<${tag}\\b[^>]*class=["'][^"']*\\b${className.replace(/[-/\\^$*+?.()|[\]{}]/g, "\\$&")}\\b[^"']*["'][^>]*>`,
    "i"
  );
  const match = withClass.exec(html);
  return match?.index ?? -1;
}

function extractBalancedElement(html: string, tag: string, startIndex: number): ExtractedElement | null {
  if (startIndex < 0) {
    return null;
  }

  const startTagMatch = new RegExp(`<${tag}\\b[^>]*>`, "i").exec(html.slice(startIndex));
  if (!startTagMatch) {
    return null;
  }

  const openStart = startIndex + startTagMatch.index;
  const openEnd = openStart + startTagMatch[0].length;
  const tokenRegex = new RegExp(`<\\/?${tag}\\b[^>]*>`, "gi");
  tokenRegex.lastIndex = openStart;

  let depth = 0;
  let match: RegExpExecArray | null = null;

  while ((match = tokenRegex.exec(html))) {
    const token = match[0];
    const isClosing = token.startsWith("</");
    depth += isClosing ? -1 : 1;

    if (depth === 0) {
      const closeEnd = match.index + token.length;
      const outer = html.slice(openStart, closeEnd);
      const inner = html.slice(openEnd, match.index);

      return {
        outer,
        inner,
        start: openStart,
        end: closeEnd
      };
    }
  }

  return null;
}

function extractPreferredContent(body: string) {
  const candidates: Array<{ tag: string; className?: string }> = [
    { tag: "main", className: "content" },
    { tag: "div", className: "page" },
    { tag: "div", className: "wrap" },
    { tag: "main" }
  ];

  for (const candidate of candidates) {
    const startIndex = findStartTagIndex(body, candidate.tag, candidate.className);
    const extracted = extractBalancedElement(body, candidate.tag, startIndex);
    if (extracted) {
      return extracted.inner;
    }
  }

  return body;
}

function stripElementAtStart(html: string, tag: string, className?: string) {
  const trimmed = html.trimStart();
  const leadingOffset = html.indexOf(trimmed);
  const startIndex = findStartTagIndex(trimmed, tag, className);

  if (startIndex !== 0) {
    return html;
  }

  const extracted = extractBalancedElement(trimmed, tag, startIndex);
  if (!extracted) {
    return html;
  }

  return html.slice(0, leadingOffset) + trimmed.slice(extracted.end).trimStart();
}

function stripFrontMatter(html: string) {
  let cleaned = html.trim();

  cleaned = stripElementAtStart(cleaned, "header");
  cleaned = stripElementAtStart(cleaned, "div", "hero");
  cleaned = stripElementAtStart(cleaned, "div", "module-header");
  cleaned = stripElementAtStart(cleaned, "section", "hero");
  cleaned = stripElementAtStart(cleaned, "div", "placeholder-card");

  cleaned = cleaned.replace(
    /^\s*<h1[^>]*>[\s\S]*?<\/h1>\s*(<p[^>]*class=["'][^"']*\bsubtitle\b[^"']*["'][^>]*>[\s\S]*?<\/p>)?/i,
    ""
  );

  return cleaned.trim();
}

function rewriteInternalLinks(html: string) {
  return docs.reduce((accumulator, doc) => {
    const escaped = doc.sourceFile.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    return accumulator.replace(new RegExp(`href=["']${escaped}["']`, "gi"), `href="/docs/${doc.slug}"`);
  }, html);
}

function sanitizeHtml(html: string) {
  return rewriteInternalLinks(
    html
      .replace(/<script[\s\S]*?<\/script>/gi, "")
      .replace(/<style[\s\S]*?<\/style>/gi, "")
      .replace(/<link[^>]*>/gi, "")
      .replace(/<nav[\s\S]*?<\/nav>/gi, "")
      .replace(/<aside[\s\S]*?<\/aside>/gi, "")
      .replace(/<!--[\s\S]*?-->/g, "")
      .replace(/\sclass=["'][^"']*\bhas-site-menu\b[^"']*["']/gi, "")
  ).trim();
}

export function getLegacyDocHtml(sourceFile: string) {
  const raw = readLegacyFile(sourceFile);
  const body = extractBody(raw);
  const extracted = extractPreferredContent(body);
  const stripped = stripFrontMatter(extracted);

  return sanitizeHtml(stripped);
}
