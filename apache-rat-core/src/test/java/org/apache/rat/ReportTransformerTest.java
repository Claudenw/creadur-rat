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

import org.apache.rat.test.utils.Resources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;


public class ReportTransformerTest {
    
    private static final String SIMPLE_CONTENT = /* 
        "<?xml version='1.0'?>" +
        "<directory name='sub'>" +
        "<standard name='Empty.txt'>" +
        "<license code='?????' name='UNKNOWN' version='' approved='false' generated='false'></license>" +
        "</standard>" +
        "<directory name='.svn' restricted='true'/>" +
        "</directory>";
        */
    " <rat-report timestamp='2024-03-29T15:04:42+01:00'>"
    + "    <resource name='src/test/resources/elements/ILoggerFactory.java' type='standard'>"
    + "        <license approval='true' family='MIT  ' id='MIT' name='The MIT License'/>"
    + "    </resource>"
    + "    <resource name='src/test/resources/elements/Image.png' type='binary'/>"
    + "    <resource name='src/test/resources/elements/LICENSE' type='notice'/>"
    + "    <resource name='src/test/resources/elements/NOTICE' type='notice'/>"
    + "    <resource name='src/test/resources/elements/Source.java' type='standard'/>"
    + "    <resource name='src/test/resources/elements/Source.java' type='standard'>"
    + "        <license approval='false' family='?????' id='?????' name='Unknown license'/>"
    + "    <sample><![CDATA[ package elements;"
    + ""
    + "/*"
    + "* This file does intentionally *NOT* contain an AL license header,"
    + "* because it is used in the test suite."
    + "*/"
    + "public class Source {"
    + ""
    + "}"
    + "]]></sample>"
    + "</resource>"
    + "    </rat-report>";

  

    @Test
    public void testTransform() throws Exception {
        StringWriter writer = new StringWriter();
        assertNotNull(writer);
        StringReader in = new StringReader(SIMPLE_CONTENT);
        ReportTransformer transformer = new ReportTransformer(writer, 
                new BufferedReader(new FileReader(Resources.getMainResourceFile("/org/apache/rat/plain-rat.xsl"))),
                in);
        transformer.transform();
    }

}
