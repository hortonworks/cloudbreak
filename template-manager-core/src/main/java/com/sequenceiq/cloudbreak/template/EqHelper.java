package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Options.Buffer;

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
    public Object apply(Object context, Options options)
            throws IOException {
        String first = options.param(0, null);

        Validate.notNull(first, "found 'null', expected 'first'");

        Buffer buffer = options.buffer();
        if (!first.equals(context.toString())) {
            buffer.append(options.inverse());
        } else {
            buffer.append(options.fn());
        }
        return buffer;
    }
}
