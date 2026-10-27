#!/usr/bin/env python3
"""Count the MinecraftModulith boundary violations left before every module can be protected.

Temporarily adds @ApplicationModule to each feature package-info.java that does not declare it
yet, compiles, restores every file it touched byte for byte, and prints the remaining violations
grouped by the module whose internals are used. Run from anywhere:

    python tools/count-boundary-violations.py [--details]
"""
import collections
import pathlib
import re
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
FEATURES = ROOT / "src/main/java/fr/elias/oessentials"
FEATURE_ANNOTATION = "@fr.elias.oessentials.platform.modularity.FeaturePackage("
ERROR = re.compile(r"^(\S.*?\.java):(\d+): error: (.*)$")
INTERNAL = re.compile(r"Module '([^']+)' cannot access internal type (\S+) from module '([^']+)'")
NOT_API = re.compile(r"Cross-module reference to (\S+) is not allowed")
MISSING = re.compile(r"Module '([^']+)' must declare dependency '([^']+)'")


def enable_all():
    originals = {}
    for info in sorted(FEATURES.glob("*/package-info.java")):
        raw = info.read_bytes()
        text = raw.decode("utf-8")
        match = re.search(r'FeaturePackage\(\s*id = "([^"]+)"', text)
        if match is None or "ApplicationModule" in text:
            continue
        originals[info] = raw
        newline = "\r\n" if "\r\n" in text else "\n"
        annotation = '@dev.oreo.modulith.core.ApplicationModule(id = "%s")' % match.group(1)
        info.write_bytes(text.replace(FEATURE_ANNOTATION, annotation + newline + FEATURE_ANNOTATION, 1)
                         .encode("utf-8"))
    return originals


def compile_all():
    with tempfile.TemporaryDirectory() as temp:
        init = pathlib.Path(temp, "maxerrs.gradle")
        init.write_text('allprojects { tasks.withType(JavaCompile).configureEach '
                        '{ options.compilerArgs += ["-Xmaxerrs", "100000"] } }\n', encoding="utf-8")
        wrapper = ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew")
        result = subprocess.run(
            [str(wrapper), "compileJava", "--rerun", "--no-configuration-cache", "--console=plain",
             "-Dorg.gradle.jvmargs=-Xmx4g", "-I", str(init)],
            cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace")
        return result.stdout + result.stderr


def main():
    originals = enable_all()
    try:
        output = compile_all()
    finally:
        for info, raw in originals.items():
            info.write_bytes(raw)

    errors = sorted({line for line in output.splitlines() if ERROR.match(line)})
    by_target = collections.Counter()
    pairs = collections.Counter()
    kinds = collections.Counter()
    for line in errors:
        message = ERROR.match(line).group(3)
        if match := INTERNAL.search(message):
            kinds["internal type"] += 1
            by_target[match.group(3)] += 1
            pairs[(match.group(1), match.group(3))] += 1
        elif NOT_API.search(message):
            kinds["not a @ModuleApi type"] += 1
        elif MISSING.search(message):
            kinds["undeclared dependency"] += 1
        else:
            kinds["other compiler error"] += 1

    print(f"Modules not yet protected: {len(originals)}")
    print(f"Remaining boundary violations: {len(errors)}")
    for kind, count in kinds.most_common():
        print(f"  {kind}: {count}")
    if by_target:
        print("\nViolations by module whose internals are used:")
        for module, count in by_target.most_common():
            print(f"  {module:<22} {count}")
    if "--details" in sys.argv:
        print("\nConsumer -> provider:")
        for (consumer, provider), count in pairs.most_common():
            print(f"  {consumer} -> {provider}: {count}")
    if not errors and "BUILD SUCCESSFUL" not in output:
        sys.stdout.buffer.write(b"\nCompilation failed for another reason:\n"
                                + output[-3000:].encode("utf-8", "replace"))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
