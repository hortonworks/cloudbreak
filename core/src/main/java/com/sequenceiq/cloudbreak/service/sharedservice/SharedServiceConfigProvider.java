package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
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
    private RdsConfigService rdsConfigService;

    public Cluster configureCluster(@Nonnull Cluster requestedCluster, User user, Workspace workspace) {
        Objects.requireNonNull(requestedCluster);
        Stack stack = requestedCluster.getStack();
        if (!Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
            Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
            if (datalakeStack != null) {
                Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(datalakeStack.getCluster().getId());
                setupRds(requestedCluster, rdsConfigs);
                setupStoragePath(requestedCluster, datalakeStack);
            }
        }
        return requestedCluster;
    }

    private void setupRds(Cluster requestedCluster, Set<RDSConfig> rdsConfigs) {
        if (requestedCluster.getRdsConfigs().isEmpty() && rdsConfigs != null) {
            requestedCluster.setRdsConfigs(remoteDataContextWorkaroundService.prepareRdsConfigs(requestedCluster, rdsConfigs));
        }
    }

    private void setupStoragePath(Cluster requestedCluster, Stack datalakeStack) {
        FileSystem fileSystem = remoteDataContextWorkaroundService.prepareFilesytem(requestedCluster, datalakeStack);
        requestedCluster.setFileSystem(fileSystem);
    }
}
