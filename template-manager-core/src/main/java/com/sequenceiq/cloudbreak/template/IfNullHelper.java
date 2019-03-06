package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Options.Buffer;

public class IfNullHelper implements Helper<Object> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Object> INSTANCE = new IfNullHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "if-null";

    @Override
    public Object apply(Object context, Options options) throws IOException {
        Buffer buffer = options.buffer();

        if (context == null) {
            buffer.append(options.fn());
        } else {
            buffer.append(options.inverse());
        }
        return buffer;
    }
}