package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RangerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "ranger";

    private static final String[] PATH = {TemplateTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties"};

    private static final String[] CONFIGURATIONS = {"db_user", "db_password", "db_name", "db_host"};

    @Value("${cb.ranger.database.user:ranger}")
    private String rangerDbUser;

    @Value("${cb.ranger.database.db:ranger}")
    private String rangerDb;

    @Value("${cb.ranger.database.port:5432}")
    private String rangerDbPort;

    @Inject
    private TemplateProcessorFactory blueprintProcessorFactory;

    private boolean isRdsConfigNeedForRangerAdmin(Blueprint blueprint) {
        TemplateTextProcessor blueprintProcessor = blueprintProcessorFactory.get(blueprint.getBlueprintText());
        return blueprintProcessor.componentExistsInBlueprint("RANGER_ADMIN")
                && !blueprintProcessor.componentExistsInBlueprint("MYSQL_SERVER")
                && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfingurations(PATH, CONFIGURATIONS));
    }

    @Override
    protected String getDbUser() {
        return rangerDbUser;
    }

    @Override
    protected String getDb() {
        return rangerDb;
    }

    @Override
    protected String getDbPort() {
        return rangerDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected RdsType getRdsType() {
        return RdsType.RANGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForRangerAdmin(blueprint);
    }
}
