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
package rat.document;

import java.io.IOException;
import java.io.Reader;

public class MockDocument implements IDocument {

    public Reader reader;
    public String name;
    public IDocumentCollection documentCollection;

    public MockDocument() {
        this(null, "name");
    }

    public MockDocument(String name) {
        this(null, name);
    }
    
    public MockDocument(Reader reader, String name) {
        super();
        this.reader = reader;
        this.name = name;
    }

    public Reader reader() throws IOException {
        return reader;
    }

    public String getName() {
        return name;
    }

    public IDocumentCollection readArchive() throws IOException {
        return documentCollection;
    }
}
