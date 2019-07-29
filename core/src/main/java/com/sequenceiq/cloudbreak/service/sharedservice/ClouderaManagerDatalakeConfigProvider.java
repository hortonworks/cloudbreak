package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
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

    @Inject
    private DatalakeConfigApiConnector datalakeConfigApiConnector;

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack) {
        Cluster cluster = datalakeStack.getCluster();
        try {
            DatalakeConfigApi connector = datalakeConfigApiConnector.getConnector(datalakeStack);
            return collectAndStoreDatalakeResources(datalakeStack, cluster, connector);
        } catch (RuntimeException ex) {
            LOGGER.warn("Datalake service discovery failed: ", ex);
            return null;
        }
    }

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack, Cluster cluster, DatalakeConfigApi connector) {
        try {
            return transactionService.required(() -> {
                DatalakeResources datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStack.getId()).orElse(null);
                if (datalakeResources == null) {
                    datalakeResources = collectDatalakeResources(datalakeStack, connector);
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

    public DatalakeResources collectDatalakeResources(Stack datalakeStack, DatalakeConfigApi connector) {
        String ambariIp = datalakeStack.getAmbariIp();
        String ambariFqdn = datalakeStack.getGatewayInstanceMetadata().isEmpty()
                ? datalakeStack.getAmbariIp() : datalakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN();
        return collectDatalakeResources(datalakeStack.getName(), ambariFqdn, ambariIp, ambariFqdn, connector);
    }

    //CHECKSTYLE:OFF
    public DatalakeResources collectDatalakeResources(String datalakeName, String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn,
            DatalakeConfigApi connector) {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setName(datalakeName);
        setupDatalakeGlobalParams(datalakeAmbariUrl, datalakeAmbariIp, datalakeAmbariFqdn, connector, datalakeResources);
        return datalakeResources;
    }

    private void setupDatalakeGlobalParams(String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn, DatalakeConfigApi connector,
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
