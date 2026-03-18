package org.apache.rat.testhelpers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.rat.ui.UIOptionCollection;

/**
 * An implementation of AbstractOptionCollection for testing.
 */
public final class BaseOptionCollection extends UIOptionCollection<BaseOption> {
    /**
     * Constructs a BaseOptionCollection with unsupportedOptions.
     */
    private BaseOptionCollection(final Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }
    public static final class Builder extends UIOptionCollection.Builder<BaseOption, Builder> {
        private Builder() {
            super();
            mapper((collection,option) -> new BaseOption((BaseOptionCollection) collection, option));
        }

        @Override
        public BaseOptionCollection build() {
            return new BaseOptionCollection(this);
        }
    }
}
