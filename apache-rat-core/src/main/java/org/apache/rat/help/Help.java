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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.help.TableDef;

import org.apache.commons.cli.help.TextStyle;
import org.apache.rat.OptionCollection;
import org.apache.rat.config.exclusion.StandardCollection;

import static java.lang.String.format;

/**
 * The help output for the command line client.
 */
public final class Help extends AbstractHelp {

    /**
     * An array of notes to go at the bottom of the help output
     */
    private static final String[] NOTES = {
            "Rat highlights possible issues.",
            "Rat reports require interpretation.",
            "Rat often requires some tuning before it runs well against a project.",
            "Rat relies on heuristics: it may miss issues"
    };

    protected static final TextStyle[] COLUMN_STYLE = {
            new TextStyle.Builder().setScaling(TextStyle.Scaling.FIXED).get(),
            new TextStyle.Builder().setLeftPad(5).setIndent(2).get()
    };

    /**
     * Creates a Help instance to write to the specified writer.
     * @param output the output to write to.
     */
    public Help(final Appendable output) {
        super(output);

    }

    /**
     * Creates a Help instance to print to the specified stream.
     * @param stream the PrintStream to write to.
     */
    public Help(final PrintStream stream) {
        this(new PrintWriter(stream));
    }

    /**
     * Print the usage to the specific PrintWriter.
     * @param opts The defined options.
     */
    public void printUsage(final Options opts) throws IOException {
        String syntax = format("java -jar apache-rat/target/apache-rat-%s.jar [options] [DIR|ARCHIVE]", versionInfo.getVersion());
        helpFormatter.printHelp(syntax, header("Available options"), opts, "", false);

        printArgumentTypes();

        printStandardCollections();

        printNotes();
    }

    public void printNotes() throws IOException {
        serializer.writePara(header("Notes"));
        serializer.writeList(true, Arrays.asList(NOTES));
    }

    /**
     * Prints the list of argument types to the writer.
     */
    public void printArgumentTypes() throws IOException {
        serializer.writeTable(TableDef.from(header("Argument Types"),
                Arrays.asList(COLUMN_STYLE),
                Arrays.asList("Name", "Meaning"),
                () -> {
                        List<List<String>> table = new ArrayList<List<String>>();
                        for (Map.Entry<String, Supplier<String>> argInfo : OptionCollection.getArgumentTypes().entrySet()) {
                            List<String> row = new ArrayList<>();
                            row.add(helpFormatter.asArgName(argInfo.getKey()));
                            row.add(argInfo.getValue().get());
                            table.add(row);
                        }
                        return table.iterator();
                    }
                ));
    }

    public void printStandardCollections() throws IOException {
        TextStyle.Builder styleBuilder = new TextStyle.Builder().setLeftPad(3).setIndent(2);
        TextStyle[] styles = {
                COLUMN_STYLE[0],
                styleBuilder.setMinWidth(25).get(),
                styleBuilder.setMinWidth(0).get(),
                styleBuilder.setAlignment(TextStyle.Alignment.CENTER).setScaling(TextStyle.Scaling.FIXED).get(),
                styleBuilder.get()
        };

        serializer.writeTable(TableDef.from(header("Standard Collections"),
                Arrays.asList(styles),
                Arrays.asList("Type", "Description", "Patterns", "Matcher","Proc"),
                () -> {
                List<List<String>> table = new ArrayList<>();
                for (StandardCollection sc : StandardCollection.values()) {
                    List<String> row = new ArrayList<>();
                    row.add(sc.name());
                    row.add(sc.desc());
                    row.add(sc.patterns().isEmpty() ? "<none>" : String.join(", ", sc.patterns()));
                    row.add(sc.hasDocumentNameMatchSupplier() ? "Yes" : "No");
                    row.add(sc.fileProcessor().hasNext() ? "Yes" : "No");
                    table.add(row);
                }
                return table.iterator();
            }));

        serializer.writeList(false, Arrays.asList("A path matcher will match specific information about the file.",
                "A file processor will process the associated \"ignore\" file for include and exclude directives"));
    }
}
