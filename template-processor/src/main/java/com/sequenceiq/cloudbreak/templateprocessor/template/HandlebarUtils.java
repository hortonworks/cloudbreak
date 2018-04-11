package com.sequenceiq.cloudbreak.templateprocessor.template;

import com.github.jknack.handlebars.Handlebars;
import org.apache.commons.lang3.StringEscapeUtils;

public final class HandlebarUtils {

    private HandlebarUtils() {
    }

    public static Handlebars handlebars() {
        Handlebars handlebars = new Handlebars();
        handlebars.with(value -> StringEscapeUtils.escapeJava(value.toString()));

        handlebars.registerHelper(EqHelper.NAME, EqHelper.INSTANCE);
        handlebars.registerHelper(NeqHelper.NAME, NeqHelper.INSTANCE);
        handlebars.registerHelperMissing((context, options) -> options.fn.text());
        return handlebars;
    }
}
