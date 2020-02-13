package com.sequenceiq.cloudbreak.tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.HandlebarTemplate;
import com.sequenceiq.cloudbreak.HandlebarUtils;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;

@Component
public class TagTemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagTemplateProcessor.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    @Inject
    private Clock clock;

    public String process(String sourceTemplate, TagPreparationObject model) throws IOException {
        long started = System.currentTimeMillis();
        String generatedTemplate = generateTemplateWithParameters(sourceTemplate, model);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.debug("The template text processed successfully by the EL based template processor under {} ms, the text after processing is: {}",
                generationTime, JsonUtil.minify(generatedTemplate));
        return generatedTemplate;
    }

    private String generateTemplateWithParameters(String sourceTemplate, TagPreparationObject model)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.apply(prepareTemplateObject(model));
    }

    private Map<String, Object> prepareTemplateObject(TagPreparationObject model) {
        Map<String, Object> templateModelContext = new HashMap<>();
        templateModelContext.put(HandleBarModelKey.ACCOUNT_ID.modelKey(), model.getAccountId());
        templateModelContext.put(HandleBarModelKey.CREATOR_CRN.modelKey(), model.getUserCrn());
        templateModelContext.put(HandleBarModelKey.CLOUD_PLATFORM.modelKey(), model.getCloudPlatform());
        templateModelContext.put(HandleBarModelKey.RESOURCE_CRN.modelKey(), model.getResourceCrn());
        templateModelContext.put(HandleBarModelKey.USER_CRN.modelKey(), model.getUserCrn());
        templateModelContext.put(HandleBarModelKey.USER_NAME.modelKey(), model.getUserName());
        templateModelContext.put(HandleBarModelKey.TIME.modelKey(), String.valueOf(clock.getCurrentTimeMillis()));
        return templateModelContext;
    }
}
