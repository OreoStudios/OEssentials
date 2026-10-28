#!/usr/bin/env python3
"""Regenerate docs/architecture/modules.mmd from the @PluginModule declarations.

    python tools/generate-module-graph.py          # rewrite the file
    python tools/generate-module-graph.py --check  # exit 1 if it is out of date
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCES = ROOT / "src/main/java/fr/elias/oessentials"
TARGET = ROOT / "docs/architecture/modules.mmd"


def declarations():
    modules = {}
    for source in SOURCES.glob("*/lifecycle/*Module.java"):
        match = re.search(r"@PluginModule\(([^)]*)\)", source.read_text(encoding="utf-8"))
        if not match:
            continue
        body = match.group(1)
        module_id = (re.search(r'value\s*=\s*"([^"]+)"', body) or re.search(r'^\s*"([^"]+)"', body)).group(1)
        listed = lambda key: re.findall(r'"([^"]+)"', (re.search(key + r"\s*=\s*\{([^}]*)\}", body) or [None, ""])[1])
        modules[module_id] = (listed("dependencies"), listed("after"))
    return modules


def render(modules):
    node = lambda module_id: module_id.replace("-", "_")
    lines = ["flowchart TD"]
    for module_id in sorted(modules):
        dependencies, after = modules[module_id]
        lines.append(f'    {node(module_id)}["{module_id}"]')
        edges = [(target, f"    {node(module_id)} --> {node(target)}") for target in after]
        for dependency in dependencies:
            target, _, selector = dependency.partition("::")
            label = f"|{selector}| " if selector else " "
            edges.append((target, f"    {node(module_id)} -->{label}{node(target)}"))
        lines.extend(line for _, line in sorted(edges))
    return "\n".join(lines) + "\n"


def main():
    text = render(declarations())
    current = TARGET.read_text(encoding="utf-8").replace("\r\n", "\n") if TARGET.exists() else ""
    if "--check" in sys.argv:
        if current != text:
            print(f"{TARGET} is out of date; run python tools/generate-module-graph.py")
            return 1
        return 0
    TARGET.write_bytes(text.replace("\n", "\r\n" if "\r\n" in TARGET.read_text(encoding="utf-8") else "\n").encode("utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
