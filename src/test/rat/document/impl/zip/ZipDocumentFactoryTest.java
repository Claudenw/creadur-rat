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
package rat.document.impl.zip;

import java.io.File;

import rat.document.IDocumentCollection;
import rat.test.utils.RATCase;

public class ZipDocumentFactoryTest extends RATCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLoadDummy() throws Exception {
        File file = new File("src/test/elements/dummy.jar");
        IDocumentCollection collection = ZipDocumentFactory.load(file);
        checkDummyJar(collection);
    }

}
