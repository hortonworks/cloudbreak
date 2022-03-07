package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    public Cluster configureCluster(@Nonnull Cluster requestedCluster, User user, Workspace workspace) {
        Objects.requireNonNull(requestedCluster);
        Stack stack = requestedCluster.getStack();
        if (!Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
            Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
            if (datalakeStack != null) {
                List<RdsConfigWithoutCluster> rdsConfigs = rdsConfigWithoutClusterService.findByClusterIdAndStatusInAndTypeIn(datalakeStack.getCluster().getId(),
                        Set.of(ResourceStatus.USER_MANAGED, ResourceStatus.DEFAULT),
                        Set.of(DatabaseType.HIVE));
                setupRds(requestedCluster, rdsConfigs);
                setupStoragePath(requestedCluster, datalakeStack);
            }
        }
        return requestedCluster;
    }

    private void setupRds(Cluster requestedCluster, List<RdsConfigWithoutCluster> rdsConfigs) {
        if (requestedCluster.getRdsConfigs().isEmpty() && rdsConfigs != null) {
            RDSConfig rdsConfig = new RDSConfig();
            rdsConfig.setId(rdsConfigs.get(0).getId());
            Set<RDSConfig> rdsConfigSet = new HashSet<>(requestedCluster.getRdsConfigs());
            rdsConfigSet.add(rdsConfig);
            requestedCluster.setRdsConfigs(rdsConfigSet);
        }
    }

    private void setupStoragePath(Cluster requestedCluster, Stack datalakeStack) {
        FileSystem fileSystem = remoteDataContextWorkaroundService.prepareFilesytem(requestedCluster, datalakeStack);
        requestedCluster.setFileSystem(fileSystem);
    }
}
