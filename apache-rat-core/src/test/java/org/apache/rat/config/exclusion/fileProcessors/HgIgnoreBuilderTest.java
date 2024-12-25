/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.config.exclusion.fileProcessors;

import java.util.ArrayList;
import org.apache.rat.config.exclusion.MatcherSet;
import org.apache.rat.document.DocumentName;
import org.apache.rat.document.DocumentNameMatcher;
import org.apache.rat.utils.ExtendedIterator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HgIgnoreBuilderTest extends AbstractIgnoreBuilderTest {

    @Test
    public void processExampleFileTest() throws IOException {
        String[] lines = {
        "# use glob syntax.", "syntax: glob", "*.elc", "*.pyc", "*~", "*.c", System.lineSeparator(),
            "# switch to regexp syntax.", "syntax: regexp", "^\\.pc" };

        List<String> matching = Arrays.asList("test.elc", "test.pyc", "subdir/test.pyc", "test.thing~", ".pc", "foo.c", "/subdir/foo.c" );
        List<String> notMatching = Arrays.asList("test.foo", "test.thing~/subdir", ".pc/subdir");

        writeFile(".hgignore", Arrays.asList(lines));

        assertCorrect(new HgIgnoreBuilder(), matching, notMatching);
    }

    @Test
    public void processDefaultFileTest() throws IOException {
        String[] lines = {"^[A-Z]*\\.txt", "[0-9]+\\.txt"};

        List<String> matching = Arrays.asList("ABIGNAME.txt", "endsIn9.txt");
        List<String> notMatching = Arrays.asList("asmallName.txt", "endsin.doc");

        writeFile(".hgignore", Arrays.asList(lines));

        assertCorrect(new HgIgnoreBuilder(), matching, notMatching);
    }
}
