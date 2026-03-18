package org.apache.rat.cli;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.rat.ui.UIOption;
import org.apache.rat.ui.UIOptionCollection;

public class CLIOptionCollection extends UIOptionCollection<CLIOption> {
    /** The Help option */
    static final Option HELP = new Option("?", "help", false, "Print help for the RAT command line interface and exit.");

    public static CLIOptionCollection INSTANCE = new CLIOptionCollection();

    private CLIOptionCollection() {
        super(new Builder().uiOption(HELP)
                .mapper(CLIOption::new));
    }

    private static class Builder extends UIOptionCollection.Builder<CLIOption, CLIOptionCollection.Builder> {
        private Builder() {
            super();
        }
    }

}
