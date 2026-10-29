package fr.elias.oessentials.autotest;

import com.oreo.lib.autotest.OreoTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/** Runs the generated contract checks; failing inputs are replayed on later runs. */
class TextHelperContractsTest {
    @Test
    void textHelpersKeepTheirContracts() throws Exception {
        var report = OreoTest.classes(TextHelperContracts.class)
                .withRegressionTests(Path.of("src/test/resources/autotest-regressions"))
                .run();
        report.writeHtml(Path.of("build/reports/autotest/text-helpers.html"));
        report.assertSuccess();
    }
}
