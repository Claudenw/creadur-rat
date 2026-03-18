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
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;
import org.apache.rat.commandline.Arg;
import org.apache.rat.ui.ArgumentTracker;
import org.apache.rat.ui.UIOptionCollection;
import org.apache.rat.utils.CasedString;

/**
 * The collection of MavenOptions equivalent to the CLI options
 * with any unsupported options removed.
 */
public final class MavenOptionCollection extends UIOptionCollection<MavenOption> {
    /** mapping of standard name to non-conflicting name. */
    private static final Map<String, String> RENAME_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("addLicense", "add-license");
        RENAME_MAP = map;
    }

    /** The instance of the MavenOptionCollection */
    public static final MavenOptionCollection INSTANCE = new Builder().build();

    /**
     * Create an Instance.
     */
    private MavenOptionCollection(final Builder builder) {
        super(builder);
    }



    public static Builder builder() {
        return new Builder();
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
    static CasedString createName(final Option option) {
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

    /**
     * The Builder for the MavenOptionCollection.
     */
    public static final class Builder extends UIOptionCollection.Builder<MavenOption, Builder> {
        private Builder() {
            super();
            unsupported(Arg.DIR).unsupported(Arg.LOG_LEVEL)
                    .mapper((collection, option) -> new MavenOption(collection, option, MavenOptionCollection.createName(option)));
        }

        public MavenOptionCollection build() {
            return new MavenOptionCollection(this);
        }
    }
}
