package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

public abstract class IfHelper implements Helper<Boolean> {

    @Override
    public abstract Object apply(Boolean context, Options options) throws IOException;

    protected Object decision(Boolean context, final Options options) throws IOException {
        if (context == null) {
            context = false;
        }

        Options.Buffer buffer = options.buffer();
        if (context) {
            buffer.append(options.inverse());
        } else {
            buffer.append(options.fn());
        }
        return buffer;
    }

    protected boolean context(Boolean context) {
        return context == null ? false : context;
    }
}
