package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class IfFalseHelper extends IfHelper {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Boolean> INSTANCE = new IfFalseHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "if-false";

    @Override
    public Object apply(Boolean context, Options options)
            throws IOException {
        return decision(context(context), options);
    }
}
