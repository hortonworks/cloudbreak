package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptorDefinition;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.servicedescriptor.ServiceDescriptorService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

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

    @Inject
    private DatalakeConfigApiConnector datalakeConfigApiConnector;

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack) {
        return collectAndStoreDatalakeResources(datalakeStack, datalakeStack.getCluster());
    }

    public DatalakeResources collectAndStoreDatalakeResources(Stack datalakeStack, Cluster cluster) {
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
                try {
                    DatalakeResources datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStack.getId()).orElse(null);
                    if (datalakeResources == null) {
                        Map<String, Map<String, String>> serviceSecretParamMap = Map.ofEntries(Map.entry(ServiceDescriptorDefinitionProvider.RANGER_SERVICE,
                                Map.ofEntries(Map.entry(ServiceDescriptorDefinitionProvider.RANGER_ADMIN_PWD_KEY, cluster.getPassword()))));
                        datalakeResources = collectDatalakeResources(datalakeStack, cluster, connector, serviceSecretParamMap);
                        datalakeResources.setDatalakeStackId(datalakeStack.getId());
                        datalakeResources.setEnvironment(datalakeStack.getEnvironment());
                        Workspace workspace = datalakeStack.getWorkspace();
                        storeDatalakeResources(datalakeResources, workspace);
                    }
                    return datalakeResources;
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (TransactionExecutionException ex) {
            LOGGER.warn("Datalake service discovery failed: ", ex);
            return null;
        }
    }

    public DatalakeResources collectDatalakeResources(Stack datalakeStack, Cluster cluster, DatalakeConfigApi connector,
            Map<String, Map<String, String>> serviceSecretParamMap) throws JsonProcessingException {
        String ambariIp = datalakeStack.getAmbariIp();
        String ambariFqdn = datalakeStack.getGatewayInstanceMetadata().isEmpty()
                ? datalakeStack.getAmbariIp() : datalakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN();
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
        return collectDatalakeResources(datalakeStack.getName(), ambariFqdn, ambariIp, ambariFqdn, connector, serviceSecretParamMap,
                cluster.getLdapConfig(), cluster.getKerberosConfig(), rdsConfigs);
    }

    //CHECKSTYLE:OFF
    public DatalakeResources collectDatalakeResources(String datalakeName, String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn,
            DatalakeConfigApi connector, Map<String, Map<String, String>> serviceSecretParamMap, LdapConfig ldapConfig, KerberosConfig kerberosConfig,
            Set<RDSConfig> rdsConfigs) throws JsonProcessingException {
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setName(datalakeName);
        Set<String> datalakeParamKeys = new HashSet<>();
        for (Entry<String, ServiceDescriptorDefinition> sddEntry : serviceDescriptorDefinitionProvider.getServiceDescriptorDefinitionMap().entrySet()) {
            datalakeParamKeys.addAll(sddEntry.getValue().getBlueprintParamKeys());
        }
        Map<String, ServiceDescriptor> serviceDescriptors = new HashMap<>();
        Map<String, String> datalakeParameters = connector.getConfigValuesByConfigIds(Lists.newArrayList(datalakeParamKeys));
        for (Entry<String, ServiceDescriptorDefinition> sddEntry : serviceDescriptorDefinitionProvider.getServiceDescriptorDefinitionMap().entrySet()) {
            ServiceDescriptor serviceDescriptor = new ServiceDescriptor();
            serviceDescriptor.setServiceName(sddEntry.getValue().getServiceName());
            Map<String, String> serviceParams = datalakeParameters.entrySet().stream()
                    .filter(dp -> sddEntry.getValue().getBlueprintParamKeys().contains(dp.getKey()))
                    .collect(Collectors.toMap(dp -> getDatalakeParameterKey(dp.getKey()), Entry::getValue));
            serviceDescriptor.setBlueprintParam(new Json(serviceParams));
            Map<String, String> serviceSecretParams = serviceSecretParamMap.getOrDefault(sddEntry.getKey(), new HashMap<>());
            serviceDescriptor.setBlueprintSecretParams(new Json(serviceSecretParams));
            Map<String, String> componentHosts = new HashMap<>();
            for (String component : sddEntry.getValue().getComponentHosts()) {
                componentHosts.put(component, String.join(",", connector.getHostNamesByComponent(component)));
            }
            serviceDescriptor.setComponentsHosts(new Json(componentHosts));
            serviceDescriptor.setDatalakeResources(datalakeResources);
            serviceDescriptors.put(serviceDescriptor.getServiceName(), serviceDescriptor);
        }
        datalakeResources.setServiceDescriptorMap(serviceDescriptors);
        setupDatalakeGlobalParams(datalakeAmbariUrl, datalakeAmbariIp, datalakeAmbariFqdn, connector, datalakeResources);
        datalakeResources.setLdapConfig(ldapConfig);
        datalakeResources.setKerberosConfig(kerberosConfig);
        if (rdsConfigs != null) {
            datalakeResources.setRdsConfigs(new HashSet<>(rdsConfigs));
        }
        return datalakeResources;
    }

    public DatalakeResources collectAndStoreDatalakeResources(String datalakeName, EnvironmentView environment, String datalakeAmbariUrl,
            String datalakeAmbariIp, String datalakeAmbariFqdn, DatalakeConfigApi connector, Map<String, Map<String, String>> serviceSecretParamMap,
            LdapConfig ldapConfig, KerberosConfig kerberosConfig, Set<RDSConfig> rdsConfigs, Workspace workspace) {
        try {
            DatalakeResources datalakeResources = collectDatalakeResources(datalakeName, datalakeAmbariUrl, datalakeAmbariIp, datalakeAmbariFqdn,
                    connector, serviceSecretParamMap, ldapConfig, kerberosConfig, rdsConfigs);
            datalakeResources.setEnvironment(environment);
            return transactionService.required(() -> {
                storeDatalakeResources(datalakeResources, workspace);
                return datalakeResources;
            });
        } catch (TransactionExecutionException | JsonProcessingException ex) {
            throw new RuntimeException("Error during datalake resource collection from ambari.", ex);
        }
    }
    //CHECKSTYLE:ON

    public SharedServiceConfigsView createSharedServiceConfigView(DatalakeResources datalakeResources) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setRangerAdminPassword((String) datalakeResources.getServiceDescriptorMap()
                .get(ServiceDescriptorDefinitionProvider.RANGER_SERVICE).getBlueprintSecretParams().getMap()
                .get(ServiceDescriptorDefinitionProvider.RANGER_ADMIN_PWD_KEY));
        sharedServiceConfigsView.setAttachedCluster(true);
        sharedServiceConfigsView.setDatalakeCluster(false);
        sharedServiceConfigsView.setDatalakeAmbariIp(datalakeResources.getDatalakeAmbariIp());
        sharedServiceConfigsView.setDatalakeAmbariFqdn(datalakeResources.getDatalakeAmbariFqdn());
        sharedServiceConfigsView.setDatalakeComponents(datalakeResources.getDatalakeComponentSet());
        sharedServiceConfigsView.setRangerAdminPort((String) datalakeResources.getServiceDescriptorMap().get(ServiceDescriptorDefinitionProvider.RANGER_SERVICE)
                .getBlueprintParams().getMap().getOrDefault(ServiceDescriptorDefinitionProvider.RANGER_HTTPPORT_KEY, DEFAULT_RANGER_PORT));
        sharedServiceConfigsView.setRangerAdminHost((String) datalakeResources.getServiceDescriptorMap().get(ServiceDescriptorDefinitionProvider.RANGER_SERVICE)
                .getComponentsHosts().getMap().get(ServiceDescriptorDefinitionProvider.RANGER_ADMIN_COMPONENT));
        return sharedServiceConfigsView;
    }

    public Map<String, String> getBlueprintConfigParameters(DatalakeResources datalakeResources, Stack workloadCluster, DatalakeConfigApi connector) {
        return getBlueprintConfigParameters(datalakeResources, workloadCluster.getCluster().getBlueprint(), connector);
    }

    public Map<String, String> getBlueprintConfigParameters(DatalakeResources datalakeResources, Blueprint workloadBlueprint,
            DatalakeConfigApi connector) {
        Map<String, String> blueprintConfigParameters = new HashMap<>(getWorkloadClusterParametersFromDatalake(workloadBlueprint, connector));
        for (ServiceDescriptor serviceDescriptor : datalakeResources.getServiceDescriptorMap().values()) {
            blueprintConfigParameters.putAll((Map) serviceDescriptor.getBlueprintParams().getMap());
            blueprintConfigParameters.putAll((Map) serviceDescriptor.getBlueprintSecretParams().getMap());
        }
        return blueprintConfigParameters;
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

    private Map<String, String> getWorkloadClusterParametersFromDatalake(Blueprint workloadBlueprint, DatalakeConfigApi connector) {
        Set<String> workloadBlueprintParamKeys =
                centralBlueprintParameterQueryService.queryDatalakeParameters(workloadBlueprint.getBlueprintText());
        return CollectionUtils.isEmpty(workloadBlueprintParamKeys)
                ? Map.of()
                : connector.getConfigValuesByConfigIds(Lists.newArrayList(workloadBlueprintParamKeys));
    }

    private void setupDatalakeGlobalParams(String datalakeAmbariUrl, String datalakeAmbariIp, String datalakeAmbariFqdn, DatalakeConfigApi connector,
            DatalakeResources datalakeResources) {
        datalakeResources.setDatalakeAmbariUrl(datalakeAmbariUrl);
        datalakeResources.setDatalakeAmbariIp(datalakeAmbariIp);
        datalakeResources.setDatalakeAmbariFqdn(StringUtils.isEmpty(datalakeAmbariFqdn) ? datalakeAmbariIp : datalakeAmbariFqdn);
        Set<String> components = new HashSet<>();
        for (Map<String, String> componentMap : connector.getServiceComponentsMap().values()) {
            components.addAll(componentMap.keySet());
        }
        datalakeResources.setDatalakeComponentSet(components);
    }

    private String getDatalakeParameterKey(String fullKey) {
        String[] parts = fullKey.split("/");
        return parts.length > 1 ? parts[1] : parts[0];
    }
}
