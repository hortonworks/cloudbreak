package com.sequenceiq.cloudbreak.template;

import com.github.jknack.handlebars.Handlebars.SafeString;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class NoEscapeHelper implements Helper<String> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<String> INSTANCE = new NoEscapeHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "noescape";

    @Override
    public Object apply(String context, Options options) {
        return new SafeString(context);
    }
}
