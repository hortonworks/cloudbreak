package com.sequenceiq.cloudbreak.blueprint.mysql;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;

@Component
public class MysqlConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlConfigProvider.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String customTextManipulation(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("MYSQL_SERVER exists in Blueprint");
        return blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER", blueprintText);
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return source.getRdsConfigs() != null
                && !source.getRdsConfigs().isEmpty()
                && blueprintProcessor.componentExistsInBlueprint("MYSQL_SERVER", blueprintText);
    }
}
