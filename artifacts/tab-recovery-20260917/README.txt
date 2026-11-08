TAB layout recovery — 2026-09-17

OreoEssentials-6.7-tab-recovery.jar restores animated decorations inside their
original columns, with dynamic expansion as players join. The header/footer
contain the full-width title and borders again. Text changes use incremental
UPDATE_DISPLAY_NAME packets. Real players are hidden with listed=false while
their profiles and chat data remain available. The periodic real-profile
removals and unconditional TAB-DEBUG logging have been removed.

Also repaired: duplicate player rows across columns, frame-dependent row
positions, column capacity with unequal decoration heights, duplicate join
listeners after reload, and delayed updates after membership changes.

Built in an isolated copy of the current workspace. Main checkout source files
have not been overwritten while Claude's editing status is unconfirmed.
The isolated source is in ../../.tab-recovery-20260917/src.
tab-recovery.patch contains the source/config changes plus regression tests;
git apply --check passed against the main checkout at preparation time.

Deployment: replace the existing OreoEssentials JAR with this JAR and restart.
Keep only one OreoEssentials JAR in the plugins directory. Startup should show:
[CustomTab] Animated column layout active (TAB recovery 2026-09-17).

The server's existing plugins/OreoEssentials/scoreboard-tab/tab.yml is preserved.
If its animation frames were removed during troubleshooting, restore the four
texts entries for the affected sections using tab-reference.yml as a reference.
Do not replace customized server configuration wholesale.

Validation: Maven package reported BUILD SUCCESS. Seven regression tests passed
for animated placement, stable slot IDs, changing frame heights, player
distribution/overflow, column capacity, header/footer content, and preserving
real profiles/actions when hiding mixed real/fake player info entries.
These tests do not reproduce client rendering or physical keyboard input.

The hold-Tab flicker is not confirmed fixed. Tapping an unbound key stops it
without movement, which strongly implicates client input handling. Test the
same server using a clean vanilla 26.2 client and report whether it persists.
Removing animations or moving column content into the header/footer was not
established as a remedy by the supplied logs and test results.
