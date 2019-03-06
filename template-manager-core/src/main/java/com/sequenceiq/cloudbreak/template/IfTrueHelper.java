package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class IfTrueHelper extends IfHelper {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Boolean> INSTANCE = new IfTrueHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "if-true";

    @Override
    public Object apply(Boolean context, Options options)
            throws IOException {
        return decision(!context(context), options);
    }
}
