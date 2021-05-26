package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class NifiRegistryRdsConfigProvider extends AbstractRdsConfigProvider {
    private static final String PILLAR_KEY = "nifi_registry";

    @Value("${cb.nifi.registry.database.user:nifi_registry}")
    private String nifiRegistryDbUser;

    @Value("${cb.nifi.registry.database.db:nifi_registry}")
    private String nifiRegistryDb;

    @Value("${cb.nifi.registry.database.port:5432}")
    private String nifiRegistryDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForNifiRegistry(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return cmTemplateProcessor.isCMComponentExistsInBlueprint("NIFI_REGISTRY_SERVER");
    }

    @Override
    protected String getDbUser() {
        return nifiRegistryDbUser;
    }

    @Override
    protected String getDb() {
        return nifiRegistryDb;
    }

    @Override
    protected String getDbPort() {
        return nifiRegistryDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.NIFIREGISTRY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForNifiRegistry(blueprint);
    }
}
