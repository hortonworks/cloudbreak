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
public class HueRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "hue";

    private static final String[] PATH = {AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hue-site"};

    private static final String[] CONFIGURATIONS = {"hue.service.jdbc.driver", "hue.service.jdbc.host", "hue.service.jdbc.port", "hue.service.jdbc.db",
            "hue.service.jdbc.username", "hue.service.jdbc.password"};

    @Value("${cb.hue.database.user:hue}")
    private String hueDbUser;

    @Value("${cb.hue.database.db:hue}")
    private String hueDb;

    @Value("${cb.hue.database.port:5432}")
    private String hueDbPort;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

    private boolean isRdsConfigNeedForHueServer(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            return blueprintProcessor.isComponentExistsInBlueprint("HUE_SERVER")
                    && blueprintProcessor.isComponentExistsInBlueprint("HUE_LOAD_BALANCER")
                    && !blueprintProcessor.isComponentExistsInBlueprint("MYSQL_SERVER")
                    && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfigurations(PATH, CONFIGURATIONS));
        }
        return blueprintProcessor.isCMComponentExistsInBlueprint("HUE_SERVER")
                && blueprintProcessor.isCMComponentExistsInBlueprint("HUE_LOAD_BALANCER");
    }

    @Override
    protected String getDbUser() {
        return hueDbUser;
    }

    @Override
    protected String getDb() {
        return hueDb;
    }

    @Override
    protected String getDbPort() {
        return hueDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.HUE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForHueServer(blueprint);
    }
}
