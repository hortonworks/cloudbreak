package com.sequenceiq.cloudbreak.handlebar;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.function.Function;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Joins a collection of items after substituting each into a string format pattern.
 *
 * For example:
 *
 * <pre>
 * {{format-join list format="https://%s" sep=";"}}
 * </pre>
 *
 * If {@code list} is the list @{code ['cloudera.com', 'example.com', 'github.com']}, the output will be
 * the string @{code "https://cloudera.com,https://example.com,https://github.com"}.
 *
 * Both format and separator are optional.
 * The default separator is {@code ","}.
 * If no format string is given, items are simply joined.
 */
public class FormatJoinHelper implements Helper<Collection<?>> {

    public static final String NAME = "format-join";

    static final FormatJoinHelper INSTANCE = new FormatJoinHelper();

    @Override
    public Object apply(Collection<?> context, Options options) {
        if (context == null || context.isEmpty()) {
            return "";
        }

        String separator = options.hash("sep", ",");
        String format = options.hash("format", null);

        Function<Object, String> formatter = format != null
                ? each -> String.format(format, each)
                : Object::toString;

        return context.stream()
                .map(formatter)
                .collect(joining(separator));
    }

}
