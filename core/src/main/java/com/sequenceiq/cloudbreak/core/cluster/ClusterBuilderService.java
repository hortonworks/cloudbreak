package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.cluster.flow.telemetry.ClusterMonitoringEngine;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.sharedservice.ClouderaManagerDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class ClusterBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBuilderService.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClouderaManagerDatalakeConfigProvider clouderaManagerDatalakeConfigProvider;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterCreationSuccessHandler clusterCreationSuccessHandler;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private ClusterMonitoringEngine clusterMonitoringEngine;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private ResourceService resourceService;

    public void startCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        connector.waitForServer(stack, true);
        boolean ldapConfigured = ldapConfigService.isLdapConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName());
        connector.changeOriginalCredentialsAndCreateCloudbreakUser(ldapConfigured);
    }

    public void buildCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());
        Cluster cluster = stack.getCluster();
        clusterService.updateCreationDateOnCluster(cluster);
        connector.waitForServer(stack, true);
        TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = loadInstanceMetadataForHostGroups(hostGroups);
        recipeEngine.executePostAmbariStartRecipes(stack, hostGroupService.getRecipesByCluster(cluster.getId()));
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        cluster.setExtendedBlueprintText(blueprintText);
        clusterService.updateCluster(cluster);
        final Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);

        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(cluster.getProxyConfigCrn(), cluster.getEnvironmentCrn());
        setupProxy(connector, proxyConfig.orElse(null));
        Set<DatalakeResources> datalakeResources = datalakeResourcesService
                .findDatalakeResourcesByWorkspaceAndEnvironment(stack.getWorkspace().getId(), stack.getEnvironmentCrn());

        Optional<Stack> sdxStack = Optional.ofNullable(datalakeResources)
                .map(Set::stream).flatMap(Stream::findFirst)
                .map(DatalakeResources::getDatalakeStackId)
                .map(stackService::getByIdWithListsInTransaction);

        String sdxContext = sdxStack
                .map(clusterApiConnectors::getConnector)
                .map(ClusterApi::getSdxContext).orElse(null);
        String sdxStackCrn = sdxStack
                .map(Stack::getResourceCrn)
                .orElse(null);
        if (telemetry.isMonitoringFeatureEnabled()) {
            connector.clusterSecurityService().setupMonitoringUser();
            clusterMonitoringEngine.installAndStartMonitoring(stack, telemetry);
        }

        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        String template = connector.clusterSetupService().prepareTemplate(instanceMetaDataByHostGroup,
                templatePreparationObject,
                sdxContext,
                sdxStackCrn,
                kerberosConfig);
        cluster.setExtendedBlueprintText(template);
        clusterService.save(cluster);
        cluster = connector.clusterSetupService().buildCluster(instanceMetaDataByHostGroup,
                templatePreparationObject,
                sdxContext,
                sdxStackCrn,
                telemetry,
                kerberosConfig,
                proxyConfig.orElse(null),
                template);
        clusterService.save(cluster);
        recipeEngine.executePostInstallRecipes(stack);
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataByHostGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        clusterCreationSuccessHandler.handleClusterCreationSuccess(instanceMetaDatas, stack.getCluster());
        if (StackType.DATALAKE == stack.getType()) {
            try {
                transactionService.required(() -> {
                    Stack stackInTransaction = stackService.getByIdWithListsInTransaction(stackId);
                    if (blueprintUtils.isClouderaManagerClusterTemplate(blueprintText)) {
                        clouderaManagerDatalakeConfigProvider.collectAndStoreDatalakeResources(stackInTransaction);
                    }
                    return null;
                });
            } catch (TransactionExecutionException e) {
                LOGGER.info("Couldn't collect Datalake paramaters", e);
            }
        }
    }

    private void setupProxy(ClusterApi connector, ProxyConfig proxyConfig) {
        if (proxyConfig != null) {
            LOGGER.info("proxyConfig is not null, setup proxy for cluster: {}", proxyConfig);
            connector.clusterSetupService().setupProxy(proxyConfig);
        } else {
            LOGGER.info("proxyConfig was not found by proxyConfigCrn");
        }
    }

    private Map<HostGroup, List<InstanceMetaData>> loadInstanceMetadataForHostGroups(Iterable<HostGroup> hostGroups) {
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            Long instanceGroupId = hostGroup.getInstanceGroup().getId();
            List<InstanceMetaData> metas = instanceMetaDataService.findAliveInstancesInInstanceGroup(instanceGroupId);
            instanceMetaDataByHostGroup.put(hostGroup, metas);
        }
        return instanceMetaDataByHostGroup;
    }
}
