/*
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 */
package org.apache.rat.help;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.help.OptionFormatter;
import org.apache.commons.cli.help.TableDef;
import org.apache.commons.cli.help.TextSerializer;
import org.apache.commons.cli.help.TextStyle;
import org.apache.commons.text.WordUtils;
import org.apache.rat.OptionCollection;
import org.apache.rat.VersionInfo;
import org.apache.rat.commandline.Arg;

import static java.lang.String.format;

/**
 * The base class to perform Help processing for programs.
 */
public abstract class AbstractHelp {
    /** Text to display when multiple options are supported */
    private static final String END_OF_OPTION_MSG = " Multiple values may be specified.  " +
            "Note that '--' or a following option is required when using this parameter.";

    /** The width of the help report in chars. */
    public static final int HELP_WIDTH = 120;
    /** The number of chars to indent output with */
    public static final int DEFAULT_COLUMN_SPACING = 4;

    /** The help formatter for this instance */
    protected final HelpFormatter helpFormatter;
    /** The version info for this instance */
    protected final VersionInfo versionInfo;

    protected final TextSerializer serializer;
    /**
     * Base class to perform help output.
     */
    protected AbstractHelp(Appendable output) {
        //ratOptionFormat = new RatOptionFormat(new DefaultOptionFormat.Builder());
        serializer = new TextSerializer(output);
        serializer.setMaxWidth(HELP_WIDTH);
        OptionFormatter.Builder optBuilder = new OptionFormatter.Builder()
                .setComparator(OptionCollection.OPTION_COMPARATOR)
                .setDeprecatedFormatFunction(DEPRECATED_MSG);
        helpFormatter = new HelpFormatter.Builder()
                .setSerializer(serializer)
                .setOptionFormatBuilder(optBuilder)
                .setDefaultTableBuilder(this::ratOptionTable)
                .build();
        versionInfo = new VersionInfo();
    }

    /** Function to format deprecated display */
    public static final Function<Option, String> DEPRECATED_MSG = o -> {
        StringBuilder sb = new StringBuilder("[").append(o.getDeprecated().toString()).append("]");
        if (o.getDescription() != null) {
            sb.append(" ").append(o.getDescription());
        }
        return sb.toString();
    };

    /**
     * Create a padding.
     * @param len The length of the padding in characters.
     * @return a string with len blanks.
     */
    public static String createPadding(final int len) {
        char[] padding = new char[len];
        Arrays.fill(padding, ' ');
        return new String(padding);
    }

    /**
     * Create a section header for the output.
     * @param txt the text to put in the header.
     * @return the Header string.
     */
    public static String header(final String txt) {
        return String.format("====== %s ======", WordUtils.capitalizeFully(txt));
    }

    public TableDef ratOptionTable(Iterable<Option> options) {
        TextStyle.Builder builder = new TextStyle.Builder().setAlignment(TextStyle.Alignment.LEFT)
                .setIndent(0).setScaling(TextStyle.Scaling.FIXED);
        List<TextStyle> styles = new ArrayList<>();
        styles.add(builder.get());
        builder.setScaling(TextStyle.Scaling.VARIABLE).setLeftPad(DEFAULT_COLUMN_SPACING);
            builder.setAlignment(TextStyle.Alignment.CENTER);
            styles.add(builder.get());

        builder.setAlignment(TextStyle.Alignment.LEFT);
        styles.add(builder.get());

        List<List<String>> rows = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Option option : options) {
            List<String> row = new ArrayList<>();
            OptionFormatter formatter = helpFormatter.getOptionFormatter(option);
            sb.setLength(0);
            sb.append(formatter.getBothOpt());
            if (option.hasArg()) {
                sb.append(" ").append(formatter.getArgName());
            }
            row.add(sb.toString());
            row.add(formatter.getSince());

            // build the description
            sb.setLength(0);
            sb.append(option.isDeprecated() ? formatter.getDeprecated() : formatter.getDescription());
             // check for multiple values
            if (option.hasArgs()) {
                sb.append(END_OF_OPTION_MSG);
            }
            // check for default value
            Arg arg = Arg.findArg(option);
            String defaultValue = arg == null ? null : arg.defaultValue();
            if (defaultValue != null) {
                sb.append(format(" (Default value = %s)", defaultValue));
            }
            row.add(sb.toString());
            rows.add(row);
        }

        return TableDef.from("", styles, Arrays.asList("Options", "Since", "Description"), rows) ;
    }
}
