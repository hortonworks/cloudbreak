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
public class OozieRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "oozie";

    private static final String[] PATH = {AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "oozie-site"};

    private static final String[] CONFIGURATIONS = {"oozie.service.JPAService.jdbc.driver", "oozie.service.JPAService.jdbc.url",
            "oozie.service.JPAService.jdbc.username", "oozie.service.JPAService.jdbc.password"};

    @Value("${cb.oozie.database.user:oozie}")
    private String oozieDbUser;

    @Value("${cb.oozie.database.db:oozie}")
    private String oozieDb;

    @Value("${cb.oozie.database.port:5432}")
    private String oozieDbPort;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

    private boolean isRdsConfigNeedForOozieServer(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            return blueprintProcessor.isComponentExistsInBlueprint("OOZIE_SERVER")
                    && !blueprintProcessor.isComponentExistsInBlueprint("MYSQL_SERVER")
                    && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfigurations(PATH, CONFIGURATIONS));
        }
        return blueprintProcessor.isCMComponentExistsInBlueprint("OOZIE_SERVER");
    }

    @Override
    protected String getDbUser() {
        return oozieDbUser;
    }

    @Override
    protected String getDb() {
        return oozieDb;
    }

    @Override
    protected String getDbPort() {
        return oozieDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.OOZIE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForOozieServer(blueprint);
    }
}