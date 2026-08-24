package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_20000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Component
public class CLOServiceRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final String PILLAR_KEY = "lakehouse_optimizer";

    @Value("${cb.lakehouse_optimizer.database.user:clo}")
    private String lakehouseOptimizerDbUser;

    @Value("${cb.lakehouse_optimizer.database.db:clo}")
    private String lakehouseOptimizerDb;

    @Value("${cb.lakehouse_optimizer.database.port:5432}")
    private String lakehouseOptimizerDbPort;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Override
    public String getDbUser() {
        return lakehouseOptimizerDbUser;
    }

    @Override
    public String getDb() {
        return lakehouseOptimizerDb;
    }

    @Override
    protected String getDbPort() {
        return lakehouseOptimizerDbPort;
    }

    @Override
    protected String getPillarKey() {
        return PILLAR_KEY;
    }

    @Override
    public DatabaseType getRdsType() {
        return DatabaseType.LAKEHOUSE_OPTIMIZER;
    }

    @Override
    protected boolean isRdsConfigNeeded(StackDtoDelegate stackDtoDelegate) {
        String blueprintText = stackDtoDelegate.getBlueprint().getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDtoDelegate.getCluster().getId());
        return clouderaManagerRepoDetails != null
                && blueprintProcessor.isServiceTypePresent("LAKEHOUSE_OPTIMIZER")
                && isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_13_2_20000);
    }
}
