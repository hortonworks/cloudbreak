package com.sequenceiq.cloudbreak.service.rdsconfig.cm;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.sdx.cdl.CdlSdxService;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;

import io.micrometer.common.util.StringUtils;

public abstract class AbstractCdlRdsConfigProvider extends AbstractRdsConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCdlRdsConfigProvider.class);

    @Inject
    private CdlSdxService cdlSdxService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    protected abstract String getDbPassword();

    protected abstract String getDbHost();

    protected abstract String getDbName();

    @Override
    public Set<RdsConfigWithoutCluster> createPostgresRdsConfigIfNeeded(StackDtoDelegate stackDto) {
        LOGGER.info("create RdsConfigFor stack: {}", stackDto.getId());
        if (isRdsConfigNeeded(stackDto) && !rdsConfigService.existsByClusterIdAndType(stackDto.getCluster().getId(), getDb())) {
            LOGGER.info("Create Rds for CDL service: {}", getRdsType());
            getConfigurationValue(stackDto);
        } else {
            super.createPostgresRdsConfigIfNeeded(stackDto);
        }
        return rdsConfigWithoutClusterService.findByClusterId(stackDto.getCluster().getId());
    }

    protected boolean isRdsConfigNeeded(StackDtoDelegate stack) {
        Optional<String> sdxCrnOptional = platformAwareSdxConnector.getSdxCrnByEnvironmentCrn(stack.getEnvironmentCrn());
        return sdxCrnOptional.filter(s -> Crn.safeFromString(s).getResourceType().equals(Crn.ResourceType.INSTANCE)).isPresent();
    }

    private void getConfigurationValue(StackDtoDelegate stack) {
        RDSConfig config = new RDSConfig();
        Map<String, String> configuration = getConfiguration(stack.getEnvironmentCrn());
        String port =  getConfigurationValue(configuration, getDbPort(), "port");
        String host = getConfigurationValue(configuration, getDbHost(), "host");
        String dbName = getConfigurationValue(configuration, getDbName(), "database name");
        String password = getConfigurationValue(configuration, getDbPassword(), "database user password");
        String user = getConfigurationValue(configuration, getDbUser(), "database username");
        config.setConnectionDriver(DatabaseVendor.POSTGRES.connectionDriver());
        config.setConnectionURL(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
        config.setConnectionPassword(password);
        config.setStatus(ResourceStatus.DEFAULT);
        config.setConnectionUserName(user);
        config.setName(String.format("%s_%s_%s", stack.getName(), stack.getId(), dbName));
        config.setType(getDb());
        Cluster cluster = new Cluster();
        cluster.setId(stack.getCluster().getId());
        config.setClusters(Set.of(cluster));
        config.setWorkspace(stack.getWorkspace());
        config.setStackVersion(stack.getStackVersion());
        config.setSslMode(RdsSslMode.DISABLED);
        config.setCreationDate(System.nanoTime());
        config.setDatabaseEngine(DatabaseVendor.POSTGRES);
        config.setStackVersion(stack.getStackVersion());
        clusterService.saveRdsConfig(config);
        clusterService.addRdsConfigToCluster(config.getId(), stack.getCluster().getId());
        LOGGER.info("created RDSConfig for service: {}", getRdsType());
    }

    private Map<String, String> getConfiguration(String environmentCrn) {
        Optional<String> sdxCrnOptional = platformAwareSdxConnector.getSdxCrnByEnvironmentCrn(environmentCrn);
        if (sdxCrnOptional.isEmpty()) {
            return Collections.emptyMap();
        }
        return cdlSdxService.getServiceConfiguration(sdxCrnOptional.get(), getDb());
    }

    private String getConfigurationValue(Map<String, String> configuration, String key, String message) {
        String value = configuration.get(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Rds Config cannot set without " + message);
        }
        return value;
    }
}
