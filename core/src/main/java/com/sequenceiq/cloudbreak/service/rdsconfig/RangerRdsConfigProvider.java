package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Component
public class RangerRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "ranger";

    private static final String[] PATH = {AmbariBlueprintTextProcessor.CONFIGURATIONS_NODE, "admin-properties", "properties"};

    private static final String[] CONFIGURATIONS = {"db_user", "db_password", "db_name", "db_host"};

    @Value("${cb.ranger.database.user:ranger}")
    private String rangerDbUser;

    @Value("${cb.ranger.database.db:ranger}")
    private String rangerDb;

    @Value("${cb.ranger.database.port:5432}")
    private String rangerDbPort;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    private boolean isRdsConfigNeedForRangerAdmin(ClusterDefinition clusterDefinition) {
        if (clusterDefinitionService.isAmbariBlueprint(clusterDefinition)) {
            String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
            AmbariBlueprintTextProcessor blueprintProcessor = ambariBlueprintProcessorFactory.get(clusterDefinitionText);
            return blueprintProcessor.isComponentExistsInBlueprint("RANGER_ADMIN")
                    && !blueprintProcessor.isComponentExistsInBlueprint("MYSQL_SERVER")
                    && !blueprintProcessor.isAllConfigurationExistsInPathUnderConfigurationNode(createPathListFromConfingurations(PATH, CONFIGURATIONS));
        }
        return false;
    }

    @Override
    protected String getDbUser() {
        return rangerDbUser;
    }

    @Override
    protected String getDb() {
        return rangerDb;
    }

    @Override
    protected String getDbPort() {
        return rangerDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    protected DatabaseType getRdsType() {
        return DatabaseType.RANGER;
    }

    @Override
    protected boolean isRdsConfigNeeded(ClusterDefinition clusterDefinition) {
        return isRdsConfigNeedForRangerAdmin(clusterDefinition);
    }
}
