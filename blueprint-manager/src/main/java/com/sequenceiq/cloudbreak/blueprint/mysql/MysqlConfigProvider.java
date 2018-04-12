package com.sequenceiq.cloudbreak.blueprint.mysql;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateTextProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class MysqlConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlConfigProvider.class);

    @Inject
    private TemplateProcessorFactory blueprintProcessorFactory;

    @Override
    public TemplateTextProcessor customTextManipulation(TemplatePreparationObject source, TemplateTextProcessor blueprintProcessor) {
        LOGGER.info("MYSQL_SERVER exists in Blueprint");
        return blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER");
    }

    @Override
    public boolean additionalCriteria(TemplatePreparationObject source, String blueprintText) {
        return source.getRdsConfigs() != null
                && !source.getRdsConfigs().isEmpty()
                && blueprintProcessorFactory.get(blueprintText).componentExistsInBlueprint("MYSQL_SERVER");
    }
}
