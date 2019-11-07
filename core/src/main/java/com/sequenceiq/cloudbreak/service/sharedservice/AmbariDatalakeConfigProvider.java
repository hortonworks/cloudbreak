package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider.RANGER_ADMIN_COMPONENT;
import static com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider.RANGER_ADMIN_PWD_KEY;
import static com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider.RANGER_HTTPPORT_KEY;
import static com.sequenceiq.cloudbreak.service.sharedservice.ServiceDescriptorDefinitionProvider.RANGER_SERVICE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.servicedescriptor.ServiceDescriptorService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class AmbariDatalakeConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDatalakeConfigProvider.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private ServiceDescriptorDefinitionProvider serviceDescriptorDefinitionProvider;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private ServiceDescriptorService serviceDescriptorService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private RdsConfigService rdsConfigService;

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack, Cluster cluster) {
        try {
            return transactionService.required(() -> {
                try {
                    DatalakeResources datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStack.getId()).orElse(null);
                    if (datalakeResources == null) {
                        datalakeResources = collectDatalakeResources(datalakeStack, cluster);
                        datalakeResources.setDatalakeStackId(datalakeStack.getId());
                        datalakeResources.setEnvironmentCrn(datalakeStack.getEnvironmentCrn());
                        Workspace workspace = datalakeStack.getWorkspace();
                        storeDatalakeResources(datalakeResources, workspace);
                    }
                    return datalakeResources;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.warn("Datalake service discovery failed: ", ex);
            return null;
        }
    }

    public DatalakeResources collectDatalakeResources(Stack datalakeStack, Cluster cluster) {
        String ambariIp = datalakeStack.getClusterManagerIp();
        String ambariFqdn = datalakeStack.getGatewayInstanceMetadata().isEmpty()
                ? datalakeStack.getClusterManagerIp() : datalakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN();
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
        return collectDatalakeResources(datalakeStack.getName(), ambariFqdn, ambariIp, ambariFqdn, rdsConfigs);
    }

    //CHECKSTYLE:OFF
    public DatalakeResources collectDatalakeResources(String datalakeName, String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn,
            Set<RDSConfig> rdsConfigs) {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setName(datalakeName);
        Map<String, ServiceDescriptor> serviceDescriptors = new HashMap<>();
        datalakeResources.setServiceDescriptorMap(serviceDescriptors);
        setupDatalakeGlobalParams(datalakeAmbariUrl, datalakeAmbariIp, datalakeAmbariFqdn, datalakeResources);
        if (rdsConfigs != null) {
            datalakeResources.setRdsConfigs(new HashSet<>(rdsConfigs));
        }
        return datalakeResources;
    }
    //CHECKSTYLE:ON

    public SharedServiceConfigsView createSharedServiceConfigView(DatalakeResources datalakeResources) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (!CollectionUtils.isEmpty(datalakeResources.getServiceDescriptorMap())) {
            sharedServiceConfigsView.setRangerAdminPassword((String) datalakeResources.getServiceDescriptorMap()
                    .get(RANGER_SERVICE)
                    .getBlueprintSecretParams()
                    .getMap()
                    .get(RANGER_ADMIN_PWD_KEY));

            sharedServiceConfigsView.setRangerAdminPort((String) datalakeResources.getServiceDescriptorMap()
                    .get(RANGER_SERVICE)
                    .getBlueprintParams()
                    .getMap()
                    .getOrDefault(RANGER_HTTPPORT_KEY, DEFAULT_RANGER_PORT));

            sharedServiceConfigsView.setRangerAdminHost((String) datalakeResources.getServiceDescriptorMap()
                    .get(RANGER_SERVICE)
                    .getComponentsHosts()
                    .getMap()
                    .get(RANGER_ADMIN_COMPONENT));
        }
        sharedServiceConfigsView.setAttachedCluster(true);
        sharedServiceConfigsView.setDatalakeCluster(false);
        sharedServiceConfigsView.setDatalakeClusterManagerIp(datalakeResources.getDatalakeAmbariIp());
        sharedServiceConfigsView.setDatalakeClusterManagerFqdn(datalakeResources.getDatalakeAmbariFqdn());
        sharedServiceConfigsView.setDatalakeComponents(datalakeResources.getDatalakeComponentSet());

        return sharedServiceConfigsView;
    }

    public Map<String, String> getAdditionalParameters(StackV4Request workloadStackRequest, DatalakeResources datalakeResources) {
        return getAdditionalParameters(workloadStackRequest.getName(), datalakeResources);
    }

    public Map<String, String> getAdditionalParameters(Stack workloadStack, DatalakeResources datalakeResources) {
        return getAdditionalParameters(workloadStack.getName(), datalakeResources);
    }

    private DatalakeResources storeDatalakeResources(DatalakeResources datalakeResources, Workspace workspace) {
        datalakeResources.setWorkspace(workspace);
        datalakeResources.getServiceDescriptorMap().forEach((k, sd) -> sd.setWorkspace(workspace));
        DatalakeResources savedDatalakeResources = datalakeResourcesService.save(datalakeResources);
        savedDatalakeResources.getServiceDescriptorMap().forEach((k, sd) -> serviceDescriptorService.save(sd));
        return savedDatalakeResources;
    }

    private Map<String, String> getAdditionalParameters(String workloadClusterName, DatalakeResources datalakeResources) {
        Map<String, String> result = new HashMap<>();
        String datalakeName = datalakeResources.getName();
        String workloadName = workloadClusterName;
        result.put("REMOTE_CLUSTER_NAME", datalakeName);
        result.put("remoteClusterName", datalakeName);
        result.put("remote.cluster.name", datalakeName);
        result.put("cluster_name", workloadName);
        result.put("cluster.name", workloadName);
        return result;
    }

    private void setupDatalakeGlobalParams(String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn,
            DatalakeResources datalakeResources) {
        datalakeResources.setDatalakeAmbariUrl(datalakeAmbariUrl);
        datalakeResources.setDatalakeAmbariIp(datalakeAmbariIp);
        datalakeResources.setDatalakeAmbariFqdn(StringUtils.isEmpty(datalakeAmbariFqdn) ? datalakeAmbariIp : datalakeAmbariFqdn);
        Set<String> components = new HashSet<>();
        datalakeResources.setDatalakeComponentSet(components);
    }
}
