package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;

@Component
public class HiveRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "hive";

    private static final String[] PATH = {AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "hive-site"};

    private static final String[] CONFIGURATIONS = {"javax.jdo.option.ConnectionURL", "javax.jdo.option.ConnectionDriverName",
            "javax.jdo.option.ConnectionUserName", "javax.jdo.option.ConnectionPassword"};

    @Value("${cb.hive.database.user:hive}")
    private String hiveDbUser;

    @Value("${cb.hive.database.db:hive}")
    private String hiveDb;

    @Value("${cb.hive.database.port:5432}")
    private String hiveDbPort;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private BlueprintService blueprintService;

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
    public Map<String, Object> createServicePillarConfigMapIfNeeded(Stack stack, Cluster cluster) {
        Map<String, Object> servicePillarConfigMap = new HashMap<>(super.createServicePillarConfigMapIfNeeded(stack, cluster));
        if (servicePillarConfigMap.containsKey(PILLAR_KEY)) {
            Map<String, Object> hiveConfigMap = (Map<String, Object>) servicePillarConfigMap.get(PILLAR_KEY);
            servicePillarConfigMap.putAll(hiveConfigMap);
        }
        return servicePillarConfigMap;
    }

    private boolean isRdsConfigNeedForHiveMetastore(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            return blueprintProcessor.isComponentExistsInBlueprint("HIVE_METASTORE")
                    && !blueprintProcessor.isComponentExistsInBlueprint("MYSQL_SERVER")
                    && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfigurations(PATH, CONFIGURATIONS));
        }
        return blueprintProcessor.isCMComponentExistsInBlueprint("HIVEMETASTORE");
    }

    @Override
    protected String getDbUser() {
        return hiveDbUser;
    }

    @Override
    protected String getDb() {
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
    protected DatabaseType getRdsType() {
        return DatabaseType.HIVE;
    }

    @Override
    protected boolean isRdsConfigNeeded(Blueprint blueprint) {
        return isRdsConfigNeedForHiveMetastore(blueprint);
    }
}
