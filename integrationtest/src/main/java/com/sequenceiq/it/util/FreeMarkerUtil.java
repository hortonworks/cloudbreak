package com.sequenceiq.it.util;

import java.util.Map;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Template;

public class FreeMarkerUtil {
    private FreeMarkerUtil() {
    }

    public static String renderTemplate(Template template, Map<String, Object> model) {
        try {
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            throw new IllegalStateException("Could not render template.", e.getCause());
        }
    }
}
