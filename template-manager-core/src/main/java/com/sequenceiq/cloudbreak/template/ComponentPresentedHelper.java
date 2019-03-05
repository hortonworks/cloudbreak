package com.sequenceiq.cloudbreak.template;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Options.Buffer;

public class ComponentPresentedHelper implements Helper<Set<String>> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<Set<String>> INSTANCE = new ComponentPresentedHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "cp";

    @Override
    public Object apply(Set<String> context, Options options)
            throws IOException {
        String first = options.param(0, null);

        Validate.notNull(first, "found 'null', expected 'first'");
        if (context == null) {
            context = new HashSet<>();
        }

        Buffer buffer = options.buffer();
        if (!context.contains(first)) {
            buffer.append(options.inverse());
        } else {
            buffer.append(options.fn());
        }
        return buffer;
    }
}
