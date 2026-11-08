OreoEssentials TAB incremental updates - 2026-09-18

Build: OreoEssentials-6.7-tab-incremental.jar

Replace the existing OreoEssentials plugin JAR with this JAR and restart the
server. Keep one OreoEssentials JAR in plugins. Existing tab.yml settings and
animation frames are preserved. No server deployment was performed here.

Startup identifier:
[CustomTab] Animated column layout active (incremental updates 2026-09-18).

Changes are now in the main checkout's source, including the previous isolated
recovery fixes. Text animations use UPDATE_DISPLAY_NAME; ping changes use
UPDATE_LATENCY. Unchanged slots and headers/footers send no updates. Actual
profile/skin changes are detected per viewer and slot, including players moving
between fixed rows. Failed slot updates can retry.

Real player profiles are retained, with UPDATE_LISTED=false hiding their rows.
The interceptor preserves profile/chat actions and handles mixed real/fake
packets. Periodic profile removals have been removed. Initialization hides real
rows once, including players already online when the module starts. Reloads
unregister old join listeners and remove owned fake entries. Viewer updates keep
rotating even when animations advance before a pass finishes. Animated frame
heights reserve stable row positions; column distribution avoids duplicates.

Validation: Maven package completed successfully on JDK 25 with:
mvn -B -ntp -o -DargLine=-Dnet.bytebuddy.experimental=true package
150 tests passed, including 20 TAB layout and packet-capture regression tests.
Build output: build.log

The physical held-Tab symptom has NOT been reproduced or confirmed fixed.
Pressing another key stopping the flicker suggests an input-repeat issue:
GLFW documents that at most one held key repeats at a time.
https://www.glfw.org/docs/latest/input#input_key
Inspection of the local vanilla 26.2 client shows ordinary player-list visibility
is controlled locally; normal held Tab is not sent as a server action.

Next diagnostic: temporarily bind List Players to an unused mouse side button
on the same client/server. If stable, investigate the keyboard input path. If
it still flickers, note whether the whole panel disappears or only rows change.

Use a full restart for installation. Switching from CUSTOM to CLASSIC live can
leave real rows unlisted until reconnect; restoring arbitrary real listings
would require coordinating with other plugins' visibility settings.
