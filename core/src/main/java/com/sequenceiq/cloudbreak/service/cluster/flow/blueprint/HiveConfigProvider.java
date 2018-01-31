package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Component
public class HiveConfigProvider {
    @Value("${cb.hive.database.user:hive}")
    private String hiveDbUser;

    @Value("${cb.hive.database.password:hive}")
    private String hiveDbPassword;

    @Value("${cb.hive.database.db:hive}")
    private String hiveDb;

    @Value("${cb.hive.database.port:5432}")
    private String hiveDbPort;

    @Value("${cb.hive.database.host:localhost}")
    private String hiveDbHost;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private HiveConfigProvider hiveConfigProvider;

    public String getHiveDbUser() {
        return hiveDbUser;
    }

    public String getHiveDbPassword() {
        return hiveDbPassword;
    }

    public String getHiveDb() {
        return hiveDb;
    }

    public String getHiveDbPort() {
        return hiveDbPort;
    }

    public String getHiveDbHost() {
        return hiveDbHost;
    }

    public boolean isRdsConfigNeedForHiveMetastore(Blueprint blueprint) {
        return blueprintProcessor.componentExistsInBlueprint("HIVE_METASTORE", blueprint.getBlueprintText())
                && !blueprintProcessor.componentExistsInBlueprint("MYSQL_SERVER", blueprint.getBlueprintText())
                && !blueprintProcessor.hivaDatabaseConfigurationExistsInBlueprint(blueprint.getBlueprintText());
    }

    public void decorateServicePillarWithPostgresIfNeeded(Map<String, SaltPillarProperties> servicePillar, Blueprint blueprint) {
        if (hiveConfigProvider.isRdsConfigNeedForHiveMetastore(blueprint)) {
            Map<String, Object> postgres = new HashMap<>();
            postgres.put("database", hiveConfigProvider.getHiveDb());
            postgres.put("user", hiveConfigProvider.getHiveDbUser());
            postgres.put("password", hiveConfigProvider.getHiveDbPassword());
            servicePillar.put("postgresql-server", new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgres)));
        }
    }
}
