/*
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 */
package org.apache.rat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.rat.commandline.OutputArgs;
import org.apache.rat.license.ILicense;
import org.apache.rat.license.ILicenseFamily;
import org.apache.rat.license.LicenseSetFactory;
import org.apache.rat.test.AbstractOptionsProvider;
import org.apache.rat.test.OptionsList;
import org.apache.rat.testhelpers.TestingLog;
import org.apache.rat.utils.DefaultLog;
import org.apache.rat.utils.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class OptionCollectionTest {

    /** The base directory for the test.  We do not use TempFile because we want the evidence of the run to exist after
     * a failure.*/
    File baseDir;

    /**
     * Constructor.
     */
    public OptionCollectionTest() {
        baseDir = new File("target/optionTools");
        baseDir.mkdirs();
    }

    /**
     * Defines the Test method that is stored in a map.
     */
    @FunctionalInterface
    public interface OptionTest {
        void test();
    }

    /**
     * This method is a known workaround for
     * {@link <a href="https://github.com/junit-team/junit5/issues/2811">junit 5 issue #2811</a> }.
     */
    @AfterEach
    @EnabledOnOs(OS.WINDOWS)
    void cleanUp() {
        System.gc();
    }

    /**
     * Returns the command line format (with '--' prefix) for the Option.
     * @param opt the option to process.
     * @return the command line option..
     */
    private static String longOpt(Option opt) {
        return "--" + opt.getLongOpt();
    }

    @Test
    public void testDeprecatedUseLogged() throws IOException {
        TestingLog log = new TestingLog();
        try {
            DefaultLog.setInstance(log);
            String[] args = {longOpt(OptionCollection.DIR), "target", "-a"};
            ReportConfiguration config = OptionCollection.parseCommands(args, o -> fail("Help printed"), true);
        } finally {
            DefaultLog.setInstance(null);
        }
        log.assertContainsExactly(1, "WARN: Option [-d, --dir] used.  Deprecated for removal since 0.17: Use '--'");
        log.assertContainsExactly(1, "WARN: Option [-a] used.  Deprecated for removal since 0.17: Use '-A' or '--addLicense'");
    }

    @Test
    public void testDirOptionCapturesDirectoryToScan() throws IOException {
        TestingLog log = new TestingLog();
        ReportConfiguration config = null;
        try {
            DefaultLog.setInstance(log);
            String[] args = {longOpt(OptionCollection.DIR), "foo"};
            config = OptionCollection.parseCommands(args, (o) -> {
            }, true);
        } finally {
            DefaultLog.setInstance(null);
        }
        assertThat(config).isNotNull();
        log.assertContainsExactly(1,"WARN: Option [-d, --dir] used.  Deprecated for removal since 0.17: Use '--'");
    }

    @Test
    public void parseExclusionsTest() {
        final Optional<IOFileFilter> filter = OptionCollection
                .parseExclusions(DefaultLog.getInstance(), Arrays.asList("", " # foo/bar", "foo", "##", " ./foo/bar"));
        assertThat(filter).isPresent();
        assertThat(filter.get()).isExactlyInstanceOf(OrFileFilter.class);
        assertTrue(filter.get().accept(baseDir, "./foo/bar" ), "./foo/bar");
        assertFalse(filter.get().accept(baseDir, "B.bar"), "B.bar");
        assertTrue(filter.get().accept(baseDir, "foo" ), "foo");
        assertFalse(filter.get().accept(baseDir, "notfoo"), "notfoo");
    }

    @Test
    public void testDefaultConfiguration() throws ParseException, IOException {
        String[] empty = {};
        CommandLine cl = new DefaultParser().parse(OptionCollection.buildOptions(), empty);
        ReportConfiguration config = OptionCollection.createConfiguration(DefaultLog.getInstance(), "", cl);
        ReportConfigurationTest.validateDefault(config);
    }

    /**
     * A parameterized test for file exclusions.
     * @param pattern The pattern to exclude
     * @param expectedPatterns The file filters that are expected to be generated from the pattern
     * @param logEntries the list of expected log entries.
     */
    @ParameterizedTest
    @MethodSource("exclusionsProvider")
    public void testParseExclusions(String pattern, List<IOFileFilter> expectedPatterns, List<String> logEntries) {
        TestingLog log = new TestingLog();
        Optional<IOFileFilter> filter = OptionCollection.parseExclusions(log, Collections.singletonList(pattern));
        if (expectedPatterns.isEmpty()) {
            assertThat(filter).isEmpty();
        } else {
            assertThat(filter).isNotEmpty();
            assertInstanceOf(OrFileFilter.class, filter.get());
            String result = filter.toString();
            for (IOFileFilter expectedFilter : expectedPatterns) {
                TextUtils.assertContains(expectedFilter.toString(), result);
            }
        }
        assertEquals(log.isEmpty(), logEntries.isEmpty());
        for (String logEntry : logEntries) {
            log.assertContains(logEntry);
        }
    }

    /** Provider for the testParseExclusions */
    public static Stream<Arguments> exclusionsProvider() {
        List<Arguments> lst = new ArrayList<>();

        lst.add(Arguments.of( "", Collections.emptyList(), Collections.singletonList("INFO: Ignored 1 lines in your exclusion files as comments or empty lines.")));

        lst.add(Arguments.of( "# a comment", Collections.emptyList(), Collections.singletonList("INFO: Ignored 1 lines in your exclusion files as comments or empty lines.")));

        List<IOFileFilter> expected = new ArrayList<>();
        String pattern = "hello.world";
        expected.add(new RegexFileFilter(pattern));
        expected.add(new NameFileFilter(pattern));
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "[Hh]ello.[Ww]orld";
        expected.add(new RegexFileFilter(pattern));
        expected.add(new NameFileFilter(pattern));
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "hell*.world";
        expected.add(new RegexFileFilter(pattern));
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        // see RAT-265 for issue
        expected = new ArrayList<>();
        pattern = "*.world";
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "hello.*";
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "?ello.world";
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "hell?.world";
        expected.add(new RegexFileFilter(pattern));
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        expected = new ArrayList<>();
        pattern = "hello.worl?";
        expected.add(new NameFileFilter(pattern));
        expected.add(WildcardFileFilter.builder().setWildcards(pattern).get());
        lst.add(Arguments.of( pattern, expected, Collections.emptyList()));

        return lst.stream();
    }

    /**
     * A paramaterized test for the options.
     * @param name The name of the test.
     * @param test the option testt to execute.
     * @throws Exception on unexpected error.
     */
    @ParameterizedTest
    @ArgumentsSource(OptionsProvider.class)
    public void testOptionsUpdateConfig(String name, OptionTest test) throws Exception {
        test.test();
    }

    /**
     * A class to provide the Options and tests to the testOptionsUpdateConfig.
     */
    static class OptionsProvider extends AbstractOptionsProvider implements ArgumentsProvider {

        /** A flag to determine if help was called */
        final AtomicBoolean helpCalled = new AtomicBoolean(false);

        /**
         * The directory to place test data in.  We do not use temp file here as we want the evidence to survive failure.
         */
        File baseDir;

        @Override
        public void helpTest() {
            String[] args = {longOpt(OptionCollection.HELP)};
            try {
                ReportConfiguration config = OptionCollection.parseCommands(args, o -> helpCalled.set(true), true);
                assertNull(config, "Should not have config");
                assertTrue(helpCalled.get(), "Help was not called");
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        /**
         * execute an "exclude" test.
         * @param args
         */
        private void execExcludeTest(String[] args) {
            try {
                ReportConfiguration config = generateConfig(args);
                IOFileFilter filter = config.getFilesToIgnore();
                assertThat(filter).isExactlyInstanceOf(OrFileFilter.class);
                assertTrue(filter.accept(baseDir, "some.foo" ), "some.foo");
                assertTrue(filter.accept(baseDir, "B.bar"), "B.bar");
                assertTrue(filter.accept(baseDir, "justbaz" ), "justbaz");
                assertFalse(filter.accept(baseDir, "notbaz"), "notbaz");
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        private void excludeFileTest(String arg) {
            File outputFile = new File(baseDir, "exclude.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("*.foo");
                fw.write(System.lineSeparator());
                fw.write("[A-Z]\\.bar");
                fw.write(System.lineSeparator());
                fw.write("justbaz");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {arg, outputFile.getPath()};
            execExcludeTest(args);
        }

        @Override
        public void excludeFileTest() {
            excludeFileTest("--exclude-file");
        }

        @Override
        protected void inputExcludeFileTest() {
            excludeFileTest("--input-exclude-file");
        }

        @Override
        public void excludeTest() {
            String[] args = {"--exclude", "*.foo", "[A-Z]\\.bar", "justbaz"};
            execExcludeTest(args);
        }

        @Override
        protected void inputExcludeTest() {
            String[] args = {"--input-exclude", "*.foo", "[A-Z]\\.bar", "justbaz"};
            execExcludeTest(args);
        }

        /*
        testMap.put("license-families-approved", this::licenseFamiliesApprovedTest);
        testMap.put("license-families-approved-file", this::licenseFamiliesApprovedFileTest);
        testMap.put("license-families-denied", this::licenseFamiliesDeniedTest);
        testMap.put("license-families-denied-file", this::licenseFamiliesDeniedFileTest);
        testMap.put("licenses", this::licensesTest);
        testMap.put("licenses-approved", this::licensesApprovedTest);
        testMap.put("licenses-approved-file", this::licensesApprovedFileTest);
        testMap.put("licenses-denied", this::licensesDeniedTest);
        testMap.put("licenses-denied-file", this::licensesDeniedFileTest);
         */

        private void execLicensesApprovedTest(String[] args) {
            try {
                ReportConfiguration config = generateConfig(args);
                SortedSet<String> result = config.getLicenseIds(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).contains("one", "two");
            } catch (IOException e) {
                fail(e.getMessage());
            }

            String[] arg2 = new String[args.length+1];
            System.arraycopy(args, 0, arg2, 0, args.length);
            arg2[args.length] = "--configuration-no-defaults";
            try {
                ReportConfiguration config = generateConfig(arg2);
                SortedSet<String> result = config.getLicenseIds(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).containsExactly("one", "two");
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        protected void licensesApprovedFileTest() {
            File outputFile = new File(baseDir, "licensesApproved.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("one");
                fw.write(System.lineSeparator());
                fw.write("two");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {"--licenses-approved-file", outputFile.getPath()};
            execLicensesApprovedTest(args);
        }

        @Override
        protected void licensesApprovedTest() {
            String[] args = {"--licenses-approved", "one", "two"};
            execLicensesApprovedTest(args);
        }

        private void execLicensesDeniedTest(String[] args) {
            try {
                ReportConfiguration config = generateConfig(args);
                assertThat(config.getLicenseIds(LicenseSetFactory.LicenseFilter.ALL)).contains("ILLUMOS");
                SortedSet<String> result = config.getLicenseIds(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).doesNotContain("ILLUMOS");
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        @Override
        protected void licensesDeniedTest() {
            String[] args = {"--licenses-denied", "ILLUMOS"};
            execLicensesDeniedTest(args);
        }

        @Override
        protected void licensesDeniedFileTest() {
            File outputFile = new File(baseDir, "licensesDenied.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("ILLUMOS");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {"--licenses-denied-file", outputFile.getPath()};
            execLicensesDeniedTest(args);
        }

        private void execLicenseFamiliesApprovedTest(String[] args) {
            String catz = ILicenseFamily.makeCategory("catz");
            try {
                ReportConfiguration config = generateConfig(args);
                SortedSet<String> result = config.getLicenseCategories(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).contains(catz);
            } catch (IOException e) {
                fail(e.getMessage());
            }

            String[] arg2 = new String[3];
            System.arraycopy(args, 0, arg2, 0, 2);
            arg2[2] = "--configuration-no-defaults";
            try {
                ReportConfiguration config = generateConfig(arg2);
                SortedSet<String> result = config.getLicenseCategories(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).containsExactly(catz);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        protected void licenseFamiliesApprovedFileTest() {
            File outputFile = new File(baseDir, "familiesApproved.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("catz");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {"--license-families-approved-file", outputFile.getPath()};
            execLicenseFamiliesApprovedTest(args);
        }

        @Override
        protected void licenseFamiliesApprovedTest() {
            String[] args = {"--license-families-approved", "catz"};
            execLicenseFamiliesApprovedTest(args);
        }

        private void execLicenseFamiliesDeniedTest(String[] args) {
            String gpl = ILicenseFamily.makeCategory("GPL");
            try {
                ReportConfiguration config = generateConfig(args);
                assertThat(config.getLicenseCategories(LicenseSetFactory.LicenseFilter.ALL)).contains(gpl);
                SortedSet<String> result = config.getLicenseCategories(LicenseSetFactory.LicenseFilter.APPROVED);
                assertThat(result).doesNotContain(gpl);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        @Override
        protected void licenseFamiliesDeniedFileTest() {
            File outputFile = new File(baseDir, "familiesApproved.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("GPL");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {"--license-families-denied-file", outputFile.getPath()};
            execLicenseFamiliesDeniedTest(args);
        }

        @Override
        protected void licenseFamiliesDeniedTest() {
            String[] args = {"--license-families-denied", "GPL"};
            execLicenseFamiliesDeniedTest(args);
        }

        /**
         * Constructor.  sets the baseDir and loads the testMap.
         */
        public OptionsProvider() {
            baseDir = new File("target/optionTools");
            baseDir.mkdirs();
        }

        /**
         * Generate a ReportConfiguration from a set of arguments.
         * Forces the {@code helpCalled} flag to be reset.
         * @param args the arguments.
         * @return A ReportConfiguration
         * @throws IOException on critical error.
         */
        private ReportConfiguration generateConfig(String[] args) throws IOException {
            helpCalled.set(false);
            ReportConfiguration config = OptionCollection.parseCommands(args, o -> helpCalled.set(true), true);
            assertFalse(helpCalled.get(), "Help was called");
            return config;
        }

        private void configTest(String arg) {
            String[] args = {arg, "src/test/resources/OptionTools/One.xml", "src/test/resources/OptionTools/Two.xml"};
            try {
                ReportConfiguration config = generateConfig(args);
                SortedSet<ILicense> set = config.getLicenses(LicenseSetFactory.LicenseFilter.ALL);
                assertTrue(set.size() > 2);
                assertTrue(LicenseSetFactory.search("ONE", "ONE", set).isPresent());
                assertTrue(LicenseSetFactory.search("TWO", "TWO", set).isPresent());

                args = new String[]{arg, "src/test/resources/OptionTools/One.xml", "src/test/resources/OptionTools/Two.xml",
                        "--configuration-no-defaults"};
                config = generateConfig(args);
                set = config.getLicenses(LicenseSetFactory.LicenseFilter.ALL);
                assertEquals(2, set.size());
                assertTrue(LicenseSetFactory.search("ONE", "ONE", set).isPresent());
                assertTrue(LicenseSetFactory.search("TWO", "TWO", set).isPresent());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void licensesTest() {
            configTest("--licenses");
        }
        @Override
        protected void configTest() {
            configTest("--config");
        }

        private void noDefaultsTest(String arg) {
            String[] args = {arg};
            try {
                ReportConfiguration config = generateConfig(args);
                assertTrue(config.getLicenses(LicenseSetFactory.LicenseFilter.ALL).isEmpty());
                config = generateConfig(new String[0]);
                assertFalse(config.getLicenses(LicenseSetFactory.LicenseFilter.ALL).isEmpty());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void noDefaultsTest() {
            noDefaultsTest("--no-default-licenses");
        }


        @Override
        protected void configurationNoDefaultsTest() {
           noDefaultsTest("--configuration-no-defaults");
        }

        @Override
        public void dryRunTest() {
            try {
                String[] args = {longOpt(OptionCollection.DRY_RUN)};
                ReportConfiguration config = generateConfig(args);
                assertTrue(config.isDryRun());
                args = new String[0];
                config = generateConfig(args);
                assertFalse(config.isDryRun());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void excludeCliTest() {
            String[] args = {longOpt(OptionCollection.EXCLUDE_CLI), "*.foo", "[A-Z]\\.bar", "justbaz"};
            execCliTest(args);
        }

        /**
         * execute an "exclude" test.
         * @param args
         */
        private void execCliTest(String[] args) {
                try {
                    ReportConfiguration config = generateConfig(args);
                    IOFileFilter filter = config.getFilesToIgnore();
                    assertThat(filter).isExactlyInstanceOf(OrFileFilter.class);
                    assertTrue(filter.accept(baseDir, "some.foo" ), "some.foo");
                    assertTrue(filter.accept(baseDir, "B.bar"), "B.bar");
                    assertTrue(filter.accept(baseDir, "justbaz" ), "justbaz");
                    assertFalse(filter.accept(baseDir, "notbaz"), "notbaz");
                } catch (IOException e) {
                    fail(e.getMessage());
                }
        }

        @Override
        public void excludeCliFileTest() {
            File outputFile = new File(baseDir, "exclude.txt");
            try (FileWriter fw = new FileWriter(outputFile)) {
                fw.write("*.foo");
                fw.write(System.lineSeparator());
                fw.write("[A-Z]\\.bar");
                fw.write(System.lineSeparator());
                fw.write("justbaz");
                fw.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] args = {longOpt(OptionCollection.EXCLUDE_FILE_CLI), outputFile.getPath()};
            execCliTest(args);
        }

        @Override
        public void forceTest() {
                String [] args =  new String[] {longOpt(OptionCollection.FORCE)};
                try {
                    ReportConfiguration config = generateConfig(args);
                    assertFalse(config.isAddingLicensesForced());
                    args = new String[]{longOpt(OptionCollection.FORCE), longOpt(OptionCollection.ADD_LICENSE)};
                    config = generateConfig(args);
                    assertTrue(config.isAddingLicensesForced());
                } catch (IOException e) {
                    fail(e.getMessage());
                }
        }

        @Override
        public void licensesTest() {
            String[] args = {longOpt(OptionCollection.LICENSES), "src/test/resources/OptionTools/One.xml", "src/test/resources/OptionTools/Two.xml"};
            try {
                ReportConfiguration config = generateConfig(args);
                SortedSet<ILicense> set = config.getLicenses(LicenseSetFactory.LicenseFilter.ALL);
                assertTrue(set.size() > 2);
                assertTrue(LicenseSetFactory.search("ONE", "ONE", set).isPresent());
                assertTrue(LicenseSetFactory.search("TWO", "TWO", set).isPresent());

                args = new String[]{longOpt(OptionCollection.LICENSES), "src/test/resources/OptionTools/One.xml", "src/test/resources/OptionTools/Two.xml", longOpt(OptionCollection.NO_DEFAULTS)};
                config = generateConfig(args);
                set = config.getLicenses(LicenseSetFactory.LicenseFilter.ALL);
                assertEquals(2, set.size());
                assertTrue(LicenseSetFactory.search("ONE", "ONE", set).isPresent());
                assertTrue(LicenseSetFactory.search("TWO", "TWO", set).isPresent());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void listLicensesTest() {
            String[] args = {longOpt(OptionCollection.LIST_LICENSES), null};
            for (LicenseSetFactory.LicenseFilter filter : LicenseSetFactory.LicenseFilter.values()) {
                try {
                    args[1] = filter.name();
                    ReportConfiguration config = generateConfig(args);
                    assertEquals(filter, config.listLicenses());
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }

        @Override
        public void listFamiliesTest() {
            String[] args = {longOpt(OptionCollection.LIST_FAMILIES), null};
            for (LicenseSetFactory.LicenseFilter filter : LicenseSetFactory.LicenseFilter.values()) {
                try {
                    args[1] = filter.name();
                    ReportConfiguration config = generateConfig(args);
                    assertEquals(filter, config.listFamilies());
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }

        public void logLevelTest() {
            String[] args = {longOpt(OptionCollection.LOG_LEVEL), null};
            Log.Level logLevel = ((DefaultLog) DefaultLog.getInstance()).getLevel();
            try {
                for (Log.Level level : Log.Level.values()) {
                    try {
                        args[1] = level.name();
                        ReportConfiguration config = generateConfig(args);
                        assertEquals(level, ((DefaultLog) DefaultLog.getInstance()).getLevel());
                    } catch (IOException e) {
                        fail(e.getMessage());
                    }
                }
            } finally {
                ((DefaultLog) DefaultLog.getInstance()).setLevel(logLevel);
            }
        }

        @Override
        public void noDefaultsTest() {
            String[] args = {longOpt(OptionCollection.NO_DEFAULTS)};
            try {
                ReportConfiguration config = generateConfig(args);
                assertTrue(config.getLicenses(LicenseSetFactory.LicenseFilter.ALL).isEmpty());
                config = generateConfig(new String[0]);
                assertFalse(config.getLicenses(LicenseSetFactory.LicenseFilter.ALL).isEmpty());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void outTest() {
            File outFile = new File( baseDir, "outexample");
            String[] args = new String[] {longOpt(OptionCollection.OUT), outFile.getAbsolutePath()};
            try {
                ReportConfiguration config = generateConfig(args);
                try (OutputStream os = config.getOutput().get()) {
                    os.write("Hello world".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try (BufferedReader reader = new BufferedReader( new InputStreamReader(Files.newInputStream(outFile.toPath())))) {
                    assertEquals("Hello world",reader.readLine());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void scanHiddenDirectoriesTest() {
            String[] args = {longOpt(OptionCollection.SCAN_HIDDEN_DIRECTORIES)};
            try {
                ReportConfiguration config = generateConfig(args);
                assertThat(config.getDirectoriesToIgnore()).isExactlyInstanceOf(FalseFileFilter.class);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void styleSheetTest() {
            String[] args = {longOpt(OptionCollection.STYLESHEET_CLI), null};
            try {
                URL url = ReportTest.class.getResource("MatcherContainerResource.txt");
                if (url == null) {
                    fail("Could not locate 'MatcherContainerResource.txt'");
                }
                for (String sheet : new String[]{"target/optionTools/stylesheet.xlt", "plain-rat", "missing-headers", "unapproved-licenses", url.getFile()}) {
                    args[1] = sheet;
                    ReportConfiguration config = generateConfig(args);
                    assertTrue(config.isStyleReport());
                }
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public void xmlTest() {
            String[] args = {longOpt(OptionCollection.XML)};
            try {
                ReportConfiguration config = generateConfig(args);
                assertFalse(config.isStyleReport());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            List<Arguments> lst = new ArrayList<>();

            for (Option option : OptionCollection.buildOptions().getOptions()) {
                if (option.getLongOpt() != null) {
                    String name = longOpt(option);
                    OptionTest test = testMap.get(option);
                    if (test == null) {
                        fail("Option "+name+" is not defined in testMap");
                    }
                    lst.add(Arguments.of(name, test));
                }
            }
            return lst.stream();
        }
    }
}
