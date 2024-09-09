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
package org.apache.rat.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.rat.Defaults;
import org.apache.rat.ReportConfiguration;
import org.apache.rat.configuration.MatcherBuilderTracker;
import org.apache.rat.license.LicenseSetFactory.LicenseFilter;
import org.apache.rat.report.xml.writer.IXmlWriter;
import org.apache.rat.report.xml.writer.impl.base.XmlWriter;
import org.apache.rat.testhelpers.XmlUtils;
import org.apache.rat.utils.DefaultLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ConfigurationReportTest {

    private ConfigurationReport report;
    private StringWriter sw;
    private IXmlWriter writer;

    String[] FAMILY_IDS = { "AL", "BSD-3", "CDDL1", "GEN", "GPL1", "GPL2", "GPL3", "MIT", "OASIS", "W3C", "W3CD", };

    @BeforeEach
    public void setup() {
        ReportConfiguration reportConfiguration = new ReportConfiguration();
        reportConfiguration.listFamilies(LicenseFilter.ALL);
        reportConfiguration.listLicenses(LicenseFilter.ALL);
        reportConfiguration.setFrom(Defaults.builder().build());

        sw = new StringWriter();
        writer = new XmlWriter(sw);
        report = new ConfigurationReport(writer, reportConfiguration);
    }

    @Test
    public void testAll() throws Exception {
        report.startReport();
        report.endReport();
        writer.closeDocument();
        String result = sw.toString();
        assertTrue(XmlUtils.isWellFormedXml(result), "Is well formed");

        XPath xPath = XPathFactory.newInstance().newXPath();
        Document doc = XmlUtils.toDom(new ByteArrayInputStream(result.getBytes()));

        // verify that all the families are there
        for (String familyName : FAMILY_IDS) {
            assertNotNull(
                    XmlUtils.getNode(doc, xPath, String.format("/rat-config/families/family[@id='%s']", familyName)),
                    () -> "Missing family " + familyName);
        }

        // verify that all the family ids are used in at least one license
        for (String familyName : FAMILY_IDS) {
            assertNotNull(
                    XmlUtils.getNode(doc, xPath, String.format("/rat-config/licenses/license[@id='%s']", familyName)),
                    () -> "Missing license " + familyName);
        }

        // verify that all matchers listed in all licenses exist in the matcher list
        for (String familyName : FAMILY_IDS) {
            List<Node> nodes = XmlUtils.getNodes(doc, xPath,
                    String.format("/rat-config/licenses/license/*", familyName));
            nodes.stream().filter(n -> n.getNodeType() == Node.ELEMENT_NODE && !"note".equals(n.getNodeName()))
                    .forEach(n -> assertNotNull(MatcherBuilderTracker.getMatcherBuilder(n.getNodeName()),
                            () -> String.format("Missing matcher named '%s'", n.getNodeName())));
        }
    }
}
