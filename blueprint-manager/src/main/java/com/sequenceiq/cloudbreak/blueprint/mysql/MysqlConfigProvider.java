package com.sequenceiq.cloudbreak.blueprint.mysql;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;

@Component
public class MysqlConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlConfigProvider.class);

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Override
    public BlueprintTextProcessor customTextManipulation(BlueprintPreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        LOGGER.info("MYSQL_SERVER exists in Blueprint");
        return blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER");
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return source.getRdsConfigs() != null
                && !source.getRdsConfigs().isEmpty()
                && blueprintProcessorFactory.get(blueprintText).componentExistsInBlueprint("MYSQL_SERVER");
    }
}
