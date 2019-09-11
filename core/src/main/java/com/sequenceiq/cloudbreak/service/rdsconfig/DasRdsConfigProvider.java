package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class DasRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "das";

    private static final String[] PATH = {AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "data_analytics_studio-database"};

    private static final String[] CONFIGURATIONS = {"data_analytics_studio_database_host", "data_analytics_studio_database_port",
            "data_analytics_studio_database_name", "data_analytics_studio_database_username", "data_analytics_studio_database_password"};

    @Value("${cb.das.database.user:das}")
    private String dasDbUser;

    @Value("${cb.das.database.db:das}")
    private String dasDb;

    @Value("${cb.das.database.port:5432}")
    private String dasDbPort;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

    @Override
    protected String getDbUser() {
        return dasDbUser;
    }

    @Override
    protected String getDb() {
        return dasDb;
    }

    @Override
    protected String getDbPort() {
        return dasDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.HIVE_DAS;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            return blueprintProcessor.isComponentExistsInBlueprint("DATA_ANALYTICS_STUDIO_EVENT_PROCESSOR")
                && blueprintProcessor.isComponentExistsInBlueprint("DATA_ANALYTICS_STUDIO_WEBAPP")
                && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfigurations(PATH, CONFIGURATIONS));
        } else {
            return blueprintProcessor.isCMComponentExistsInBlueprint("DAS_EVENT_PROCESSOR")
                || blueprintProcessor.isCMComponentExistsInBlueprint("DAS_WEBAPP");
        }
    }
}
