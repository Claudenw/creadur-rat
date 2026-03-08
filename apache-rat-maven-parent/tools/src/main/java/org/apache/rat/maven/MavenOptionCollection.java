/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.maven;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.rat.commandline.Arg;
import org.apache.rat.ui.AbstractOptionCollection;
import org.apache.rat.ui.ArgumentTracker;
import org.apache.rat.utils.CasedString;

/**
 * The collection of MavenOptions equivalent to the CLI options
 * with any unsupported options removed.
 */
public class MavenOptionCollection extends AbstractOptionCollection<MavenOption> {
    /** Set of CLI options that are not supported by Maven. */
    private static final UnmodifiableSet<Option> UNSUPPORTED_OPTIONS;
    /** Set of Additional options defined for Maven */
    private static final Options ADDITIONAL_OPTIONS;
    /** mapping of standard name to non-conflicting name. */
    private static final Map<String, String> RENAME_MAP;
    /** mapping of option to default values */
    private final Map<Option, String> defaultOverride;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("addLicense", "add-license");
        RENAME_MAP = (UnmodifiableMap<String, String>) UnmodifiableMap.unmodifiableMap(map);
        Set<Option> unsupportedOptions = new HashSet<>();
        unsupportedOptions.addAll(Arg.DIR.group().getOptions());
        unsupportedOptions.addAll(Arg.LOG_LEVEL.group().getOptions());
        UNSUPPORTED_OPTIONS = (UnmodifiableSet<Option>) UnmodifiableSet.unmodifiableSet(unsupportedOptions);
        ADDITIONAL_OPTIONS = new Options();
    }

    /**
     * Create an Instance.
     */
    public MavenOptionCollection() {
        super(UNSUPPORTED_OPTIONS, new Options().addOptions(ADDITIONAL_OPTIONS));
        defaultOverride = new HashMap<>();
    }

    /**
     * Provides a new name for an option if it is renamed in the collection.
     * @param name the option name.
     * @return the collection name, may be the same as the option name.
     */
    static String rename(final String name) {
        return StringUtils.defaultIfEmpty(RENAME_MAP.get(name), name);
    }

    /**
     * Creates the name for the option based on rules for conversion of CLI option names.
     * @param option the standard option.
     * @return the new Option name as a CasedString.
     */
    CasedString createName(final Option option) {
        List<String> pluralEndings = List.of("approved", "denied");
        String name = rename(ArgumentTracker.extractKey(option));
        CasedString casedName = new CasedString(CasedString.StringCase.KEBAB, name);
        String[] segments = casedName.getSegments();
        String lastSegment = segments[segments.length - 1];
        if (option.hasArgs()) {
            if (!lastSegment.endsWith("s") && !pluralEndings.contains(lastSegment)) {
                segments[segments.length - 1] += "s";
                casedName = new CasedString(CasedString.StringCase.KEBAB, segments);
            }
        }
        return casedName.as(CasedString.StringCase.CAMEL);
    }

    @Override
    protected Function<Option, MavenOption> getMapper() {
        return option -> new MavenOption(this, option, createName(option));
    }

    @Override
    protected Map<Option, String> defaultOverrides() {
        return defaultOverride;
    }

    @Override
    public void addOverride(final Option option, final String value) {
        defaultOverride.put(option, value);
    }
}
