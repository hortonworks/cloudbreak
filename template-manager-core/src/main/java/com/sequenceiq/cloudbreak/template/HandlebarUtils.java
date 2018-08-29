package com.sequenceiq.cloudbreak.template;

import org.apache.commons.lang3.StringEscapeUtils;

import com.github.jknack.handlebars.Handlebars;

public final class HandlebarUtils {

    private HandlebarUtils() {
    }

    public static Handlebars handlebars() {
        Handlebars handlebars = new Handlebars();
        handlebars.with(value -> StringEscapeUtils.escapeJava(value.toString()));

        handlebars.registerHelper(EqHelper.NAME, EqHelper.INSTANCE);
        handlebars.registerHelper(NeqHelper.NAME, NeqHelper.INSTANCE);
        handlebars.registerHelper(IfTrueHelper.NAME, IfTrueHelper.INSTANCE);
        handlebars.registerHelper(IfFalseHelper.NAME, IfFalseHelper.INSTANCE);
        handlebars.registerHelper(ComponentPresentedHelper.NAME, ComponentPresentedHelper.INSTANCE);
        handlebars.registerHelper(NoEscapeHelper.NAME, NoEscapeHelper.INSTANCE);
        handlebars.registerHelperMissing((context, options) -> options.fn.text());
        return handlebars;
    }
}
