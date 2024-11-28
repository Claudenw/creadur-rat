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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.rat.api.Document.Type;
import org.apache.rat.api.RatException;
import org.apache.rat.commandline.StyleSheets;
import org.apache.rat.document.FileDocument;
import org.apache.rat.document.DocumentName;
import org.apache.rat.license.ILicenseFamily;
import org.apache.rat.report.claim.ClaimStatistic;
import org.apache.rat.test.utils.Resources;
import org.apache.rat.testhelpers.TextUtils;
import org.apache.rat.testhelpers.XmlUtils;
import org.apache.rat.walker.DirectoryWalker;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tests the output of the Reporter.
 */
public class ReporterTest {
    @TempDir
    File tempDirectory;
    final String basedir;

    ReporterTest() {
        basedir = new File(Files.currentFolder(), "target/test-classes/elements").getPath();
    }

    @Test
    public void testExecute() throws RatException, ParseException {
        File output = new File(tempDirectory, "testExecute");

        CommandLine cl = new DefaultParser().parse(OptionCollection.buildOptions(), new String[]{"--output-style", "xml", "--output-file", output.getPath()});
        ReportConfiguration config = OptionCollection.createConfiguration(basedir, cl);
        ClaimStatistic statistic = new Reporter(config).execute();

        assertThat(statistic.getCounter(Type.ARCHIVE)).isEqualTo(1);
        assertThat(statistic.getCounter(Type.BINARY)).isEqualTo(2);
        assertThat(statistic.getCounter(Type.GENERATED)).isEqualTo(1);
        assertThat(statistic.getCounter(Type.NOTICE)).isEqualTo(2);
        assertThat(statistic.getCounter(Type.STANDARD)).isEqualTo(8);
        assertThat(statistic.getCounter(Type.UNKNOWN)).isEqualTo(0);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.APPROVED)).isEqualTo(9);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.ARCHIVES)).isEqualTo(1);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.BINARIES)).isEqualTo(2);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.DOCUMENT_TYPES)).isEqualTo(5);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.GENERATED)).isEqualTo(1);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.LICENSE_CATEGORIES)).isEqualTo(5);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.LICENSE_NAMES)).isEqualTo(6);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.NOTICES)).isEqualTo(2);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.STANDARDS)).isEqualTo(8);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.UNAPPROVED)).isEqualTo(2);
        assertThat(statistic.getCounter(ClaimStatistic.Counter.UNKNOWN)).isEqualTo(2);

        List<Type> typeList = statistic.getDocumentTypes();
        assertThat(typeList).isEqualTo(Arrays.asList(Type.ARCHIVE, Type.BINARY, Type.GENERATED, Type.NOTICE, Type.STANDARD));

        TreeMap<String, Integer> expected = new TreeMap<>();
        expected.put("Unknown license", 2);
        expected.put("Apache License Version 2.0", 5);
        expected.put("The MIT License", 1);
        expected.put("BSD 3 clause", 1);
        expected.put("Generated Files", 1);
        expected.put("The Telemanagement Forum License", 1);
        TreeMap<String, Integer> actual = new TreeMap<>();

        for (String licenseName : statistic.getLicenseNames()) {
            actual.put(licenseName, statistic.getLicenseNameCount(licenseName));
        }
        assertThat(actual).isEqualTo(expected);

        expected.clear();
        expected.put("?????", 2);
        expected.put("AL   ", 5);
        expected.put("BSD-3", 2);
        expected.put("GEN  ", 1);
        expected.put("MIT  ", 1);
        actual.clear();
        for (String licenseCategory : statistic.getLicenseFamilyCategories()) {
            actual.put(licenseCategory, statistic.getLicenseCategoryCount(licenseCategory));
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testOutputOption() throws Exception {
        File output = new File(tempDirectory, "test");
        CommandLine cl = new DefaultParser().parse(OptionCollection.buildOptions(), new String[]{"-o", output.getCanonicalPath()});
        ReportConfiguration config = OptionCollection.createConfiguration(basedir, cl);
        new Reporter(config).output();
        assertThat(output.exists()).isTrue();
        String content = FileUtils.readFileToString(output, StandardCharsets.UTF_8);
        TextUtils.assertPatternInTarget("^! Unapproved:\\s*2 ", content);
        assertThat(content).contains("/Source.java");
        assertThat(content).contains("/sub/Empty.txt");
    }

    @Test
    public void testDefaultOutput() throws Exception {
        File output = new File(tempDirectory, "testDefaultOutput");

        PrintStream origin = System.out;
        try (PrintStream out = new PrintStream(output)) {
            System.setOut(out);
            CommandLine cl = new DefaultParser().parse(OptionCollection.buildOptions(), new String[]{});
            ReportConfiguration config = OptionCollection.createConfiguration(basedir, cl);
            new Reporter(config).output();
        } finally {
            System.setOut(origin);
        }
        assertThat(output).exists();
        String content = FileUtils.readFileToString(output, StandardCharsets.UTF_8);
        verifyStandardContent(content);
    }

    @Test
    public void testXMLOutput() throws Exception {
        File output = new File(tempDirectory, "testXMLOutput");

        CommandLine cl = new DefaultParser().parse(OptionCollection.buildOptions(), new String[]{"--output-style", "xml", "--output-file", output.getPath()});
        ReportConfiguration config = OptionCollection.createConfiguration(basedir, cl);
        new Reporter(config).output();

        assertThat(output).exists();
        Document doc = XmlUtils.toDom(java.nio.file.Files.newInputStream(output.toPath()));
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@approval='false']");
        assertThat(nodeList.getLength()).isEqualTo(2);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@id='AL']");

        assertThat(nodeList.getLength()).isEqualTo(5);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@id='MIT']");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@id='BSD-3']");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@id='TMF']");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/license[@id='?????']");
        assertThat(nodeList.getLength()).isEqualTo(2);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='STANDARD']");
        assertThat(nodeList.getLength()).isEqualTo(8);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='ARCHIVE']");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='BINARY']");
        assertThat(nodeList.getLength()).isEqualTo(2);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='GENERATED']");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='UNKNOWN']");
        assertThat(nodeList.getLength()).isEqualTo(0);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='NOTICE']");
        assertThat(nodeList.getLength()).isEqualTo(2);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource/sample");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath, "/rat-report/resource[@type='GENERATED']/license/notes");
        assertThat(nodeList.getLength()).isEqualTo(1);

        nodeList = XmlUtils.getNodeList(doc, xPath,
                "/rat-report/resource[@name='/Source.java']/sample");
        assertThat(nodeList.getLength()).isEqualTo(1);
    }

    /**
     * Finds a node via xpath on the document. And then checks family, approval and
     * type of elements of the node.
     *
     * @param doc The document to check/
     * @param xpath the XPath instance to use.
     * @param resource the xpath statement to locate the node.
     * @param licenseInfo the license info for the node. (may = null)
     * @param type the type of resource located.
     * @param hasSample true if a sample from the document should be present.
     * @throws Exception on XPath error.
     */
    private static void checkNode(final Document doc, final XPath xpath, final String resource, final LicenseInfo licenseInfo,
                                  final String type, final boolean hasSample) throws Exception {
        XmlUtils.getNode(doc, xpath, String.format("/rat-report/resource[@name='%s'][@type='%s']", resource, type));
        if (licenseInfo != null) {
            XmlUtils.getNode(doc, xpath,
                    String.format("/rat-report/resource[@name='%s'][@type='%s']/license[@id='%s'][@family='%s']",
                            resource, type, licenseInfo.id, licenseInfo.family));
            XmlUtils.getNode(doc, xpath,
                    String.format("/rat-report/resource[@name='%s'][@type='%s']/license[@id='%s'][@approval='%s']",
                            resource, type, licenseInfo.id, licenseInfo.approval));
            if (licenseInfo.hasNotes) {
                XmlUtils.getNode(doc, xpath,
                        String.format("/rat-report/resource[@name='%s'][@type='%s']/license[@id='%s']/notes", resource,
                                type, licenseInfo.id));
            }
        }
        if (hasSample) {
            XmlUtils.getNode(doc, xpath,
                    String.format("/rat-report/resource[@name='%s'][@type='%s']/sample", resource, type));
        }
    }

    private ReportConfiguration initializeConfiguration() throws IOException {
        Defaults defaults = Defaults.builder().build();
        final File elementsFile = new File(Resources.getResourceDirectory("elements/Source.java"));
        final ReportConfiguration configuration = new ReportConfiguration();
        configuration.setFrom(defaults);
        DocumentName documentName = new DocumentName(elementsFile);
        configuration.setReportable(new DirectoryWalker(new FileDocument(documentName, elementsFile,
                configuration.getNameMatcher(documentName))));
        return configuration;
    }

    private void verifyStandardContent(final String document) {
        TextUtils.assertPatternInTarget("^  Notices:\\s*2 ", document);
        TextUtils.assertPatternInTarget("^  Binaries:\\s*2 ", document);
        TextUtils.assertPatternInTarget("^  Archives:\\s*1 ", document);
        TextUtils.assertPatternInTarget("^  Standards:\\s*8 ", document);
        TextUtils.assertPatternInTarget("^  Generated:\\s*1 ", document);
        TextUtils.assertPatternInTarget("^! Unapproved:\\s*2 ", document);
        TextUtils.assertPatternInTarget("^  Unknown:\\s*2 ", document);

        TextUtils.assertPatternInTarget("^Apache License Version 2.0: 5 ", document);
        TextUtils.assertPatternInTarget("^BSD 3 clause: 1 ", document);
        TextUtils.assertPatternInTarget("^Generated Files: 1 ", document);
        TextUtils.assertPatternInTarget("^The MIT License: 1 ", document);
        TextUtils.assertPatternInTarget("^The Telemanagement Forum License: 1 ", document);
        TextUtils.assertPatternInTarget("^Unknown license: 2 ", document);

        TextUtils.assertPatternInTarget("^\\Q?????\\E: 2 ", document);
        TextUtils.assertPatternInTarget("^AL   : 5 ", document);
        TextUtils.assertPatternInTarget("^BSD-3: 2 ", document);
        TextUtils.assertPatternInTarget("^GEN  : 1 ", document);
        TextUtils.assertPatternInTarget("^MIT  : 1 ", document);

        TextUtils.assertPatternInTarget(
                "^Files with unapproved licenses\\s+\\*+\\s+" //
                        + "\\Q/Source.java\\E\\s+" //
                        + "\\Q/sub/Empty.txt\\E\\s",
                document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.ARCHIVE, "/dummy.jar"),
                document);
        TextUtils.assertPatternInTarget(
                ReporterTestUtils.documentOut(true, Type.STANDARD, "/ILoggerFactory.java")
                        + ReporterTestUtils.licenseOut("MIT", "The MIT License"),
                document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.BINARY, "/Image.png"),
                document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.NOTICE, "/LICENSE"),
                document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.NOTICE, "/NOTICE"), document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(false, Type.STANDARD, "/Source.java")
                + ReporterTestUtils.UNKNOWN_LICENSE, document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.STANDARD, "/Text.txt")
                + ReporterTestUtils.APACHE_LICENSE, document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.STANDARD, "/Xml.xml")
                + ReporterTestUtils.APACHE_LICENSE, document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.STANDARD, "/buildr.rb")
                + ReporterTestUtils.APACHE_LICENSE, document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.STANDARD, "/TextHttps.txt")
                + ReporterTestUtils.APACHE_LICENSE, document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(true, Type.STANDARD, "/tri.txt")
                + ReporterTestUtils.APACHE_LICENSE + ReporterTestUtils.licenseOut("BSD-3", "BSD 3 clause")
                + ReporterTestUtils.licenseOut("BSD-3", "TMF", "The Telemanagement Forum License"), document);
        TextUtils.assertPatternInTarget(ReporterTestUtils.documentOut(false, Type.STANDARD, "/sub/Empty.txt")
                + ReporterTestUtils.UNKNOWN_LICENSE, document);
    }

    private Validator initValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(Reporter.class.getResourceAsStream("/org/apache/rat/rat-report.xsd"));
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    @Test
    public void xmlReportTest() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ReportConfiguration configuration = initializeConfiguration();
        configuration.setStyleSheet(StyleSheets.XML.getStyleSheet());
        configuration.setOut(() -> out);
        new Reporter(configuration).output();
        Document doc = XmlUtils.toDom(new ByteArrayInputStream(out.toByteArray()));

        XPath xPath = XPathFactory.newInstance().newXPath();

        XmlUtils.getNode(doc, xPath, "/rat-report[@timestamp]");

        LicenseInfo apacheLic = new LicenseInfo("AL", true, false);
        checkNode(doc, xPath, "/ILoggerFactory.java", new LicenseInfo("MIT", true, false),
                "STANDARD", false);
        checkNode(doc, xPath, "/Image.png", null, "BINARY", false);
        checkNode(doc, xPath, "/LICENSE", null, "NOTICE", false);
        checkNode(doc, xPath, "/NOTICE", null, "NOTICE", false);
        checkNode(doc, xPath, "/Source.java", new LicenseInfo("?????", false, false),
                "STANDARD", true);
        checkNode(doc, xPath, "/Text.txt", apacheLic, "STANDARD", false);
        checkNode(doc, xPath, "/TextHttps.txt", apacheLic, "STANDARD", false);
        checkNode(doc, xPath, "/Xml.xml", apacheLic, "STANDARD", false);
        checkNode(doc, xPath, "/buildr.rb", apacheLic, "STANDARD", false);
        checkNode(doc, xPath, "/dummy.jar", null, "ARCHIVE", false);
        checkNode(doc, xPath, "/sub/Empty.txt", new LicenseInfo("?????", false, false),
                "STANDARD", false);
        checkNode(doc, xPath, "/tri.txt", apacheLic, "STANDARD", false);
        checkNode(doc, xPath, "/tri.txt", new LicenseInfo("BSD-3", true, false), "STANDARD",
                false);
        checkNode(doc, xPath, "/tri.txt", new LicenseInfo("TMF", "BSD-3", true, false),
                "STANDARD", false);
        checkNode(doc, xPath, "/generated.txt", new LicenseInfo("GEN", true, true),
                "GENERATED", false);
        NodeList nodeList = (NodeList) xPath.compile("/rat-report/resource").evaluate(doc, XPathConstants.NODESET);
        assertThat(nodeList.getLength()).isEqualTo(14);
        Validator validator = initValidator();
        try {
            validator.validate(new DOMSource(doc));
        } catch (SAXException e) {
            fail("Missing properties?", e);
        }
    }

    private static final String NL = System.lineSeparator();
    private static final String SEPARATOR = "*****************************************************";
    private static final String HEADER = SEPARATOR + NL + //
            "Summary" + NL + //
            SEPARATOR + NL + //
            "Generated at: ";

    @Test
    public void plainReportTest() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ReportConfiguration configuration = initializeConfiguration();
        configuration.setOut(() -> out);
        new Reporter(configuration).output();

        out.flush();
        String document = out.toString();

        TextUtils.assertNotContains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", document);
        assertThat(document).as(() -> "'Generated at' is not present in \n" + document).startsWith(HEADER);


        verifyStandardContent(document);
    }

    @Test
    public void unapprovedLicensesReportTest() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ReportConfiguration configuration = initializeConfiguration();
        configuration.setOut(() -> out);
        configuration.setStyleSheet(this.getClass().getResource("/org/apache/rat/unapproved-licenses.xsl"));
        new Reporter(configuration).output();

        out.flush();
        String document = out.toString();

        TextUtils.assertContains("Generated at: ", document );
        TextUtils.assertPatternInTarget("\\Q/Source.java\\E$", document);
        TextUtils.assertPatternInTarget("\\Q/sub/Empty.txt\\E", document);
    }

    @Test
    public void counterMaxTest() throws Exception {
        ReportConfiguration config = initializeConfiguration();
        Reporter reporter = new Reporter(config);
        reporter.output();
        assertThat(config.getClaimValidator().hasErrors()).isTrue();
        assertThat(config.getClaimValidator().isValid(ClaimStatistic.Counter.UNAPPROVED, reporter.getClaimsStatistic().getCounter(ClaimStatistic.Counter.UNAPPROVED)))
                .isFalse();

        config = initializeConfiguration();
        config.getClaimValidator().setMax(ClaimStatistic.Counter.UNAPPROVED, 2);
        reporter = new Reporter(config);
        reporter.output();
        assertThat(config.getClaimValidator().hasErrors()).isFalse();
        assertThat(config.getClaimValidator().isValid(ClaimStatistic.Counter.UNAPPROVED, reporter.getClaimsStatistic().getCounter(ClaimStatistic.Counter.UNAPPROVED)))
                .isTrue();
    }

    private static class LicenseInfo {
        final String id;
        final String family;
        final boolean approval;
        final boolean hasNotes;

        LicenseInfo(String id, boolean approval, boolean hasNotes) {
            this(id, id, approval, hasNotes);
        }

        LicenseInfo(String id, String family, boolean approval, boolean hasNotes) {
            this.id = id;
            this.family = ILicenseFamily.makeCategory(family);
            this.approval = approval;
            this.hasNotes = hasNotes;
        }
    }
}
