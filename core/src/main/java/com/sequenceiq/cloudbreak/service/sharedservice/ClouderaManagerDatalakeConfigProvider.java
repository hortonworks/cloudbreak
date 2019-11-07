package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class ClouderaManagerDatalakeConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDatalakeConfigProvider.class);

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private RdsConfigService rdsConfigService;

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack) {
        Cluster cluster = datalakeStack.getCluster();
        try {
            return collectAndStoreDatalakeResources(datalakeStack, cluster);
        } catch (RuntimeException ex) {
            LOGGER.warn("Datalake service discovery failed: ", ex);
            return null;
        }
    }

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack, Cluster cluster) {
        try {
            return transactionService.required(() -> {
                DatalakeResources datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStack.getId()).orElse(null);
                if (datalakeResources == null) {
                    datalakeResources = collectDatalakeResources(datalakeStack);
                    datalakeResources.setDatalakeStackId(datalakeStack.getId());
                    datalakeResources.setEnvironmentCrn(datalakeStack.getEnvironmentCrn());
                    Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
                    if (rdsConfigs != null) {
                        datalakeResources.setRdsConfigs(new HashSet<>(rdsConfigs));
                    }
                    Workspace workspace = datalakeStack.getWorkspace();
                    storeDatalakeResources(datalakeResources, workspace);
                }
                return datalakeResources;
            });
        } catch (TransactionService.TransactionExecutionException ex) {
            LOGGER.warn("Datalake service discovery failed: ", ex);
            return null;
        }
    }

    public DatalakeResources collectDatalakeResources(Stack datalakeStack) {
        String ambariIp = datalakeStack.getClusterManagerIp();
        String ambariFqdn = datalakeStack.getGatewayInstanceMetadata().isEmpty()
                ? datalakeStack.getClusterManagerIp() : datalakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN();
        return collectDatalakeResources(datalakeStack.getName(), ambariFqdn, ambariIp, ambariFqdn);
    }

    //CHECKSTYLE:OFF
    public DatalakeResources collectDatalakeResources(String datalakeName, String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn) {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setName(datalakeName);
        setupDatalakeGlobalParams(datalakeAmbariUrl, datalakeAmbariIp, datalakeAmbariFqdn, datalakeResources);
        return datalakeResources;
    }

    private void setupDatalakeGlobalParams(String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn,
            DatalakeResources datalakeResources) {
        datalakeResources.setDatalakeAmbariUrl(datalakeAmbariUrl);
        datalakeResources.setDatalakeAmbariIp(datalakeAmbariIp);
        datalakeResources.setDatalakeAmbariFqdn(StringUtils.isEmpty(datalakeAmbariFqdn) ? datalakeAmbariIp : datalakeAmbariFqdn);
        datalakeResources.setDatalakeComponentSet(Set.of());
    }

    private DatalakeResources storeDatalakeResources(DatalakeResources datalakeResources, Workspace workspace) {
        datalakeResources.setWorkspace(workspace);
        return datalakeResourcesService.save(datalakeResources);
    }
}
