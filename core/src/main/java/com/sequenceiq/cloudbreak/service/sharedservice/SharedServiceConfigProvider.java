package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private ClusterService clusterService;

    @Inject
    private SecretService secretService;

    @Inject
    private DatabaseCommon dbCommon;

    public Cluster configureCluster(Cluster requestedCluster) {
        Objects.requireNonNull(requestedCluster);
        Stack stack = requestedCluster.getStack();
        if (StackType.WORKLOAD.equals(stack.getType())) {
            Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn());
            if (sdxBasicView.isPresent()) {
                switch (sdxBasicView.get().platform()) {
                    case PAAS -> configureClusterByPaasDatalake(requestedCluster, stack, sdxBasicView.get());
                    default -> LOGGER.info("Data Lake platform is not recognized, skipping setup regarding shared filesystem and RDS!");
                }
                platformAwareSdxConnector.getSdxFileSystemViewByEnvironmentCrn(stack.getEnvironmentCrn()).ifPresent(sdxFileSystemView ->
                        requestedCluster.setFileSystem(
                                remoteDataContextWorkaroundService.prepareFilesystem(requestedCluster, sdxFileSystemView, sdxBasicView.get().crn())));
            }
        }
        return requestedCluster;
    }

    private void configureClusterByPaasDatalake(Cluster requestedCluster, Stack stack, SdxBasicView sdxBasicView) {
        Stack datalakeStack = stackService.getByCrn(sdxBasicView.crn());
        if (datalakeStack != null) {
            setupHmsRdsByPaasDatalakeStack(datalakeStack, requestedCluster);
        } else {
            setupHmsRdsByRemoteDataContext(stack, requestedCluster, sdxBasicView);
        }
    }

    private void setupHmsRdsByPaasDatalakeStack(Stack datalakeStack, Cluster requestCluster) {
        List<RdsConfigWithoutCluster> rdsConfigs = rdsConfigWithoutClusterService.findByClusterIdAndStatusInAndTypeIn(datalakeStack.getCluster().getId(),
                Set.of(ResourceStatus.USER_MANAGED, ResourceStatus.DEFAULT),
                Set.of(DatabaseType.HIVE));
        setRdsConfigsForCluster(requestCluster, rdsConfigs);
    }

    private void setRdsConfigsForCluster(Cluster requestedCluster, List<RdsConfigWithoutCluster> rdsConfigs) {
        if (requestedCluster.getRdsConfigs().isEmpty() && rdsConfigs != null) {
            RDSConfig rdsConfig = new RDSConfig();
            rdsConfig.setId(rdsConfigs.getFirst().getId());
            Set<RDSConfig> rdsConfigSet = new HashSet<>(requestedCluster.getRdsConfigs());
            rdsConfigSet.add(rdsConfig);
            requestedCluster.setRdsConfigs(rdsConfigSet);
        }
    }

    private void setupHmsRdsByRemoteDataContext(StackDtoDelegate stack, Cluster requestedCluster, SdxBasicView sdxBasicView) {
        String databaseType = DatabaseType.HIVE.name();
        Map<String, String> configuration = platformAwareSdxConnector.getHmsServiceConfig(sdxBasicView.crn());
        if (MapUtils.isNotEmpty(configuration)) {
            String host = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_HOST);
            String port = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_PORT);
            String dbName = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_NAME);
            String connectionUrl = dbCommon.getJdbcConnectionUrl("postgresql", host, Integer.parseInt(port), Optional.of(dbName));
            Optional<RdsConfigWithoutCluster> rdsConfigWithoutCluster = rdsConfigWithoutClusterService.findByConnectionUrlAndType(connectionUrl, databaseType);
            if (rdsConfigWithoutCluster.isPresent()) {
                setRdsConfigsForCluster(requestedCluster, List.of(rdsConfigWithoutCluster.get()));
            } else {
                // we should reach this point only in case of CDL
                String userName = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_USER);
                String passwordVaultPath = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_PASSWORD);

                RDSConfig config = new RDSConfig();
                config.setConnectionDriver(DatabaseVendor.POSTGRES.connectionDriver());
                config.setConnectionURL(connectionUrl);
                config.setConnectionPassword(secretService.getSecretFromExternalVault(passwordVaultPath));
                config.setStatus(ResourceStatus.DEFAULT);
                config.setConnectionUserName(userName);
                config.setName(String.format("%s_%s_%s", stack.getName(), stack.getId(), dbName));
                config.setType(databaseType);
                config.setClusters(Set.of(requestedCluster));
                config.setWorkspace(stack.getWorkspace());
                config.setStackVersion(stack.getStackVersion());
                config.setSslMode(RdsSslMode.DISABLED);
                config.setCreationDate(System.nanoTime());
                config.setDatabaseEngine(DatabaseVendor.POSTGRES);
                config.setStackVersion(stack.getStackVersion());
                clusterService.saveRdsConfig(config);
                LOGGER.info("created RDSConfig for service: {}", databaseType);
                requestedCluster.getRdsConfigs().add(config);
            }
        }
    }

    private String getHmsServiceConfigValue(Map<String, String> configuration, String key) {
        return Optional.ofNullable(configuration.get(key))
                .orElseThrow(() -> new NoSuchElementException(String.format("%s is not found in remote data context!", key)));
    }
}
