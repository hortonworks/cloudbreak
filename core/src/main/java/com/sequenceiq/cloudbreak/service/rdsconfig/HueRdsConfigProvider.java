package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class HueRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "hue";

    @Value("${cb.hue.database.user:hue}")
    private String hueDbUser;

    @Value("${cb.hue.database.db:hue}")
    private String hueDb;

    @Value("${cb.hue.database.port:5432}")
    private String hueDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForHueServer(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.doesCMComponentExistsInBlueprint("HUE_SERVER")
                && blueprintProcessor.doesCMComponentExistsInBlueprint("HUE_LOAD_BALANCER");
    }

    @Override
    public String getDbUser() {
        return hueDbUser;
    }

    @Override
    public String getDb() {
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
    public DatabaseType getRdsType() {
        return DatabaseType.HUE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForHueServer(blueprint);
    }
}
