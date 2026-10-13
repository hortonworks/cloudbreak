package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter.SmartRouterRoles.SMARTROUTER_SERVER;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Component
public class SmartRouterRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "smartrouter";

    @Value("${cb.smartrouter.database.user:smartrouter}")
    private String smartRouterDbUser;

    @Value("${cb.smartrouter.database.db:smartrouter}")
    private String smartRouterDb;

    @Value("${cb.smartrouter.database.port:5432}")
    private String smartRouterDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.SMART_ROUTER;
    }

    @Override
    public String getDbUser() {
        return smartRouterDbUser;
    }

    @Override
    public String getDb() {
        return smartRouterDb;
    }

    @Override
    protected String getDbPort() {
        return smartRouterDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected boolean isRdsConfigNeeded(StackDtoDelegate stackDtoDelegate) {
        Blueprint blueprint = stackDtoDelegate.getBlueprint();
        if (blueprint == null) {
            return false;
        }
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText());
        return blueprintProcessor.doesCMComponentExistsInBlueprint(SMARTROUTER_SERVER);
    }
}
