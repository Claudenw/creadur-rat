
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
package rat.header;

import java.io.StringReader;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class HeaderMatcherWithBeansTest extends TestCase {

    int capacity;
    HeaderMatcher matcher;
    SimpleCharFilter filter;
    HeaderBean[] beans;
    
    protected void setUp() throws Exception {
        super.setUp();
        HeaderBean[] beans = {
                new HeaderBean(),
                new HeaderBean(),
                new HeaderBean()
            };
        this.beans = beans;
        capacity = 20;
        filter = new SimpleCharFilter();
        matcher = new HeaderMatcher(filter, 20, beans);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testNulls() throws Exception {
        beans[0].setMatch(false);
        beans[1].setMatch(true);
        beans[2].setMatch(false);
        StringReader reader = new StringReader("Whatever");
        matcher.read(reader);   
        assertFalse("State preserved", beans[0].isMatch());
        assertTrue("State preserved", beans[1].isMatch());
        assertFalse("State preserved", beans[2].isMatch());
        beans[0].setMatch(true);
        beans[1].setMatch(false);
        beans[2].setMatch(true);
        assertTrue("State preserved", beans[0].isMatch());
        assertFalse("State preserved", beans[1].isMatch());
        assertTrue("State preserved", beans[2].isMatch());
    }
    
    public void testMatches() throws Exception {
        beans[0].setHeaderPattern(Pattern.compile("What(.*)"));
        beans[1].setHeaderPattern(Pattern.compile("(.*)ever"));
        beans[2].setHeaderPattern(Pattern.compile("What"));
        StringReader reader = new StringReader("Whatever");
        matcher.read(reader);   
        assertTrue("Match header pattern", beans[0].isMatch());
        assertTrue("Match header pattern", beans[1].isMatch());
        assertFalse("Match header pattern", beans[2].isMatch());
    }
}
