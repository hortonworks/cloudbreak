package com.sequenceiq.cloudbreak.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Options.Buffer;

public abstract class IfHelper implements Helper<Boolean> {

    protected Object decision(Boolean context, Options options) throws IOException {
        if (context == null) {
            context = false;
        }

        Buffer buffer = options.buffer();
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
