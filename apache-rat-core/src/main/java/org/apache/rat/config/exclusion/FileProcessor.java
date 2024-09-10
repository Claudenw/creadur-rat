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
package org.apache.rat.config.exclusion;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.rat.config.exclusion.plexus.MatchPattern;
import org.apache.rat.config.exclusion.plexus.SelectorUtils;
import org.apache.rat.document.impl.DocumentName;
import org.apache.rat.utils.iterator.WrappedIterator;

import static java.lang.String.format;

/**
 * An interface that defines the FileProcessor.  The file priocessor reads the file specified in the DocumentName.
 * It must return a list of fully qualified stings for the {@link MatchPattern} to process.  It may return either
 * Ant or Regex style strings, or a mixture of both.  See {@link SelectorUtils} for a description of the formats.
 */
@FunctionalInterface
public interface FileProcessor extends Function<DocumentName, List<String>> {
    /** A String.format pattern to print a regex string */
    String REGEX_FMT = "%%regex[%s]";

    /** an Empty file processor -- return no entries.*/
    FileProcessor EMPTY = DocumentName -> Collections.emptyList();

    /**
     * Create a virtual file processor out of a list of file patterns.
     * @param patterns the patterns to simulate the file from.
     * @return A file processor that processes the patterns.
     */
    static FileProcessor from(Iterable<String> patterns) {
        return new FileProcessor() {
            @Override
            public List<String> apply(DocumentName documentName) {
                return WrappedIterator.create(patterns.iterator())
                        .map(entry -> FileProcessor.localizePattern(documentName, entry))
                        .map(DocumentName::name)
                        .toList();
            }
        };
    }

    /**
     * Allows modification of the file entry to match the {@link MatchPattern} format.
     * Default implementation returns the @{code entry} argument.
     * @param documentName the name of the document that the file was read from.
     * @param entry the entry from that document.
     * @return the modified string or null to skip the string.
     */
    default String modifyEntry(DocumentName documentName, String entry) {
        return entry;
    }

    /**
     * Modifies the {@link MatchPattern} formatted {@code pattern} argument by expanding the pattern in the
     * by adjusting the pattern to include the basename from the {@code basename} argument.
     * @param pattern the pattern to format.
     * @return the completely formatted pattern
     */
    static DocumentName localizePattern(final DocumentName baseName, final String pattern) {
        String normalizedPattern = SelectorUtils.extractPattern(pattern, baseName.dirSeparator());
        if (SelectorUtils.isRegexPrefixedPattern(pattern)) {
            StringBuilder sb = new StringBuilder();
            sb.append(SelectorUtils.REGEX_HANDLER_PREFIX)
                    .append("\\Q").append(baseName.baseName())
                    .append(baseName.dirSeparator())
                    .append("\\E").append(normalizedPattern)
                    .append(SelectorUtils.PATTERN_HANDLER_SUFFIX);
            return new DocumentName(sb.toString(), baseName.baseName(), baseName.dirSeparator(), baseName.isCaseSensitive());
        }
        return baseName.baseDocumentName().resolve(normalizedPattern);
    }
}