# Gradle Build Scans (Develocity)

OEssentials uses `gradle/actions/setup-gradle@v6` in `.github/workflows/ci.yml`.
The action can automatically publish a detailed Build Scan for each Gradle
invocation in trusted GitHub Actions runs: push to `main`, manual runs, and
pull requests opened from branches in this same repository.

Build Scans help diagnose task failures, dependency resolution problems, test
results, build timings, build configuration and environment details.

## One-time opt-in: publishing and Terms of Use

**Publishing is OFF by default.** Build Scans send build metadata to the
external public Gradle service, and accepting its Terms of Use is a decision
for the repository owner.

1. Read [Gradle's Terms of Use](https://gradle.com/terms-of-service).
2. If you choose to accept them and enable external publication, open
   **OreoStudios/OEssentials → Settings → Secrets and variables → Actions → Variables**.
3. Create a repository variable named `OESSENTIALS_BUILD_SCAN_TOS_ACCEPTED`
   with the value `yes` (lowercase).
4. Open **Actions → Build & Test → Run workflow**, choosing `main`.
5. Open the new run. Its **Gradle build, tests & Modulith verification**
   job summary should contain the Build Scan link after a Gradle execution.
6. Follow Gradle's activation instructions (including email verification
   when required) to view the scan.

The `setup-gradle` action handles the scan integration; **do not** also add
`--scan` to the CI Gradle command. Publishing works for failed Gradle builds
as well as successful ones when the scan service is available.

To disable publication, delete that repository variable or change it to
anything other than `yes`. PRs from forks never publish scans using this
workflow's configuration.

## Privacy and access

Public Build Scans are hosted by Gradle/Develocity and can be opened by
anyone who has the scan URL. They can include names of dependencies,
build commands, system information, timings and other environment metadata.
Never publish secrets in Gradle command lines or build output.

No scan is published when the consent variable is not enabled.
Local developer builds can opt in separately with
`./gradlew build --scan`, and follow the Gradle prompt.

## Artifacts are unchanged

- `oessentials-plugin-jar`: installable shaded plugin
- `oessentials-libraries`: separate dependency JARs for developers

Build Scans are **web reports**, not additional JARs or GitHub artifacts.

Official documentation:
- https://github.com/gradle/actions/blob/main/docs/setup-gradle.md#publishing-to-scansgradlecom
- https://docs.gradle.org/current/userguide/build_scans.html
