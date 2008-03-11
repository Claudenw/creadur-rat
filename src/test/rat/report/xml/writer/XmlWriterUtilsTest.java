/*
 * Copyright 2006 Robert Burrell Donkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package rat.report.xml.writer;

import java.io.StringWriter;

import junit.framework.TestCase;
import rat.report.xml.writer.impl.base.XmlWriter;

public class XmlWriterUtilsTest extends TestCase {

    StringWriter out;
    IXmlWriter writer;
    
    protected void setUp() throws Exception {
        super.setUp();
        out = new StringWriter();
        writer = new XmlWriter(out);
        writer.openElement("alpha");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWriteTrue() throws Exception {
        XmlWriterUtils.writeAttribute(writer, "name", true);
        assertEquals("Attribute written as True", "<alpha name='true'", out.toString());
    }

    public void testWriteFalse() throws Exception {
        XmlWriterUtils.writeAttribute(writer, "name", false);
        assertEquals("Attribute written as False", "<alpha name='false'", out.toString());
    }
}
