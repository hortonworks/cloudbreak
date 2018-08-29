package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public class EqHelper implements Helper<Object> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new EqHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "eq";

    @Override
    public Object apply(final Object context, final Options options)
            throws IOException {
        String first = options.param(0, null);

        Validate.notNull(first, "found 'null', expected 'first'");

        Options.Buffer buffer = options.buffer();
        if (!first.equals(context.toString())) {
            buffer.append(options.inverse());
        } else {
            buffer.append(options.fn());
        }
        return buffer;
    }
}
