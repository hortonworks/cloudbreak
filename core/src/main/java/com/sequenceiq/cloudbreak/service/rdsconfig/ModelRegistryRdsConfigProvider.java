package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class ModelRegistryRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "modelregistry";

    @Value("${cb.hive.database.user:modelregistry}")
    private String modelRegistryDbUser;

    @Value("${cb.hive.database.db:modelregistry}")
    private String modelRegistryDb;

    @Value("${cb.hive.database.port:5432}")
    private String modelRegistryDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private boolean isRdsConfigNeedForModelRegistry(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("MODEL_REGISTRY");
    }

    @Override
    protected String getDbUser() {
        return modelRegistryDbUser;
    }

    @Override
    protected String getDb() {
        return modelRegistryDb;
    }

    @Override
    protected String getDbPort() {
        return modelRegistryDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.MODEL_REGISTRY;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForModelRegistry(blueprint);
    }
}
