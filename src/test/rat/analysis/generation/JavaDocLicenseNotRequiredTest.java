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
package rat.analysis.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import rat.report.claim.impl.xml.MockClaimReporter;

public class JavaDocLicenseNotRequiredTest extends TestCase {

    MockClaimReporter reporter;
    JavaDocLicenseNotRequired license;
    
    protected void setUp() throws Exception {
        super.setUp();
        license = new JavaDocLicenseNotRequired();
        reporter = new MockClaimReporter();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMatchIndexDoc() throws Exception {
        boolean result = readAndMatch("index.html");
        assertTrue("Is a javadoc", result);
    }

    public void testMatchClassDoc() throws Exception {
        boolean result = readAndMatch("ArchiveElement.html");
        assertTrue("Is a javadoc", result);
    }

    public void testMatchNonJavaDoc() throws Exception {
        boolean result = readAndMatch("notjavadoc.html");
        assertFalse("Not javadocs and so should return null", result);
    }
    
    boolean readAndMatch(String name) throws Exception {
        File file = new File("src/test/javadocs/" + name);
        boolean result = false;
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = in.readLine();
        while (line != null && !result) {
            result = license.match("subject", line, reporter);
            line = in.readLine();
        }
        return result;
    }
}
