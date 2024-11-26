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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.rat.config.exclusion.plexus.MatchPattern;
import org.apache.rat.config.exclusion.plexus.SelectorUtils;
import org.apache.rat.document.DocumentName;
import org.apache.rat.document.DocumentNameMatcher;

/**
 * The file processor reads the file specified in the DocumentName.
 * It must return a list of fully qualified strings for the {@link MatchPattern} to process. It may return either
 * Ant or Regex style strings, or a mixture of both. See {@link SelectorUtils} for a description of the formats.
 * It may also generate custom DocumentNameMatchers which are added to the customMatchers instance variable.
 */
public class FileProcessor implements Function<DocumentName, Iterable<String>> {
    /** A String format pattern to print a regex string */
    public static final String REGEX_FMT = "%%regex[%s]";
    /** an empty file processor returning no entries.*/
    public static final FileProcessor EMPTY = new FileProcessor();
    /** The list of patterns that will be converted into DocumentNameMatchers */
    private final List<String> patterns = new ArrayList<>();
    /** The collection of custom DocumentNameMatchers generated by this processor */
    protected final List<DocumentNameMatcher> customMatchers = new ArrayList<>();

    /**
     * Protected constructor.
     */
    protected FileProcessor() {
    }

    /**
     * Create a file processor out of a list of file patterns.
     * @param patterns the patterns to simulate the file from.
     */
    public FileProcessor(final Iterable<String> patterns) {
        patterns.forEach(this.patterns::add);
    }

    @Override
    public Iterable<String> apply(final DocumentName documentName) {
        return patterns.stream().map(entry -> localizePattern(documentName, entry)).collect(Collectors.toList());
    }

    /**
     * If this FileProcessor builds custom matchers to handles special cases this method returns them
     * to the processing stream.
     * @return A collection of DocumentNameMatchers.  Default returns an empty list.
     */
    public final Iterable<DocumentNameMatcher>  customDocumentNameMatchers() {
        return customMatchers;
    }


    /**
     * Allows modification of the file entry to match the {@link MatchPattern} format.
     * Default implementation returns the @{code entry} argument.
     * @param documentName the name of the document that the file was read from.
     * @param entry the entry from that document.
     * @return the modified string or null to skip the string.
     */
    protected String modifyEntry(final DocumentName documentName, final String entry) {
        return entry;
    }

    /**
     * Modifies the {@link MatchPattern} formatted {@code pattern} argument by expanding the pattern and
     * by adjusting the pattern to include the basename from the {@code documentName} argument.
     * @param documentName the name of the file being read.
     * @param pattern the pattern to format.
     * @return the completely formatted pattern
     */
    protected final String localizePattern(final DocumentName documentName, final String pattern) {
        boolean prefix = pattern.startsWith("!");
        String workingPattern = prefix ? pattern.substring(1) : pattern;
        String normalizedPattern = SelectorUtils.extractPattern(workingPattern, documentName.getDirectorySeparator());
        StringBuilder sb = new StringBuilder();
        if (SelectorUtils.isRegexPrefixedPattern(workingPattern)) {
            sb.append(prefix ? "!" : "")
                    .append(SelectorUtils.REGEX_HANDLER_PREFIX)
                    .append("\\Q").append(documentName.getBaseName())
                    .append(documentName.getDirectorySeparator())
                    .append("\\E").append(normalizedPattern)
                    .append(SelectorUtils.PATTERN_HANDLER_SUFFIX);
            return sb.toString();
        } else {
            sb.append(documentName.getBaseName())
                    .append(documentName.getDirectorySeparator()).append(normalizedPattern);
//                    .append(documentName.getDirectorySeparator().equals("/") ? normalizedPattern :
//                            normalizedPattern.replace(documentName.getDirectorySeparator(), "/"));
            System.out.println("LOCALIZING " + documentName + " setName: " + sb.toString());
            return (prefix ? "!" : "") + DocumentName.builder(documentName).setName(sb.toString()).build().getName();
        }
    }
}
