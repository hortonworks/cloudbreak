package com.sequenceiq.cloudbreak.blueprint.template;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;

public final class HandlebarUtils {

    private HandlebarUtils() {
    }

    public static Handlebars handlebars() {
        Handlebars handlebars = new Handlebars();
        handlebars.with(EscapingStrategy.NOOP);

        handlebars.registerHelper(EqHelper.NAME, EqHelper.INSTANCE);
        handlebars.registerHelper(NeqHelper.NAME, NeqHelper.INSTANCE);
        handlebars.registerHelperMissing((context, options) -> options.fn.text());
        return handlebars;
    }
}
