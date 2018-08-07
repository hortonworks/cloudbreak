package com.sequenceiq.cloudbreak.blueprint.template;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

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
    public Object apply(Set<String> context, final Options options)
            throws IOException {
        String first = options.param(0, null);

        notNull(first, "found 'null', expected 'first'");
        if (context == null) {
            context = new HashSet<>();
        }

        Options.Buffer buffer = options.buffer();
        if (!context.contains(first)) {
            buffer.append(options.inverse());
        } else {
            buffer.append(options.fn());
        }
        return buffer;
    }
}
