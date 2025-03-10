package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Component
public class HiveRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "hive";

    @Value("${cb.hive.database.user:hive}")
    private String hiveDbUser;

    @Value("${cb.hive.database.db:hive}")
    private String hiveDb;

    @Value("${cb.hive.database.port:5432}")
    private String hiveDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    /**
     *  For 2.4 compatibility we also need some extra field to be in the pillar so it will look like this:
     *
     *  postgres:
     *   hive:
     *     database: hive
     *     password: asdf
     *     user: hive
     *  database: hive
     *  password: asdf
     *  user: hive
     */
    @Override
    public Map<String, Object> createServicePillarConfigMapIfNeeded(StackDto stackDto) {
        Map<String, Object> servicePillarConfigMap = new HashMap<>(super.createServicePillarConfigMapIfNeeded(stackDto));
        if (servicePillarConfigMap.containsKey(PILLAR_KEY)) {
            Map<String, Object> hiveConfigMap = (Map<String, Object>) servicePillarConfigMap.get(PILLAR_KEY);
            servicePillarConfigMap.putAll(hiveConfigMap);
        }
        return servicePillarConfigMap;
    }

    private boolean isRdsConfigNeedForHiveMetastore(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.doesCMComponentExistsInBlueprint("HIVEMETASTORE");
    }

    @Override
    public String getDbUser() {
        return hiveDbUser;
    }

    @Override
    public String getDb() {
        return hiveDb;
    }

    @Override
    protected String getDbPort() {
        return hiveDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.HIVE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint, boolean hasGateway) {
        return isRdsConfigNeedForHiveMetastore(blueprint);
    }
}
