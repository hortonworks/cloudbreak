package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.FinalizeClusterInstallHandlerService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.cluster.flow.telemetry.ClusterMonitoringEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

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
    private TransactionService transactionService;

    @Inject
    private FinalizeClusterInstallHandlerService finalizeClusterInstallHandlerService;

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
    private DatalakeService datalakeService;

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

    public void waitForClusterManager(Long stackId) throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        clusterService.updateCreationDateOnCluster(stack.getCluster());
        clusterApiConnectors
                .getConnector(stack)
                .waitForServer(stack, true);
    }

    public void validateLicence(Long stackId) {
        getClusterSetupService(stackId).validateLicence();
    }

    public void configureManagementServices(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(
                stack.getCluster().getProxyConfigCrn(),
                stack.getCluster().getEnvironmentCrn());

        getClusterSetupService(stack).configureManagementServices(conversionService.convert(stack, TemplatePreparationObject.class),
                getSdxContext(stack),
                getSdxStackCrn(stack),
                componentConfigProviderService.getTelemetry(stackId),
                proxyConfig.orElse(null));
    }

    public void configureSupportTags(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        getClusterSetupService(stack).configureSupportTags(conversionService.convert(stack, TemplatePreparationObject.class));
    }

    public void updateConfig(Long stackId) {
        getClusterSetupService(stackId).updateConfig();
    }

    public void refreshParcelRepos(Long stackId) {
        getClusterSetupService(stackId).refreshParcelRepos();
    }

    public void startManagementServices(Long stackId) {
        getClusterSetupService(stackId).startManagementServices();
    }

    public void suppressWarnings(Long stackId) {
        getClusterSetupService(stackId).suppressWarnings();
    }

    public void configureKerberos(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        getClusterSetupService(stackId).configureKerberos(kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null));
    }

    public void installCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        getClusterSetupService(stack).installCluster(stack.getCluster().getExtendedBlueprintText());
    }

    public void autoConfigureCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        getClusterSetupService(stack).autoConfigureClusterManager();
    }

    public void prepareProxyConfig(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);

        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(
                stack.getCluster().getProxyConfigCrn(),
                stack.getCluster().getEnvironmentCrn());
        if (proxyConfig.isPresent()) {
            LOGGER.info("proxyConfig is not null, setup proxy for cluster: {}", proxyConfig);
            getClusterSetupService(stack).setupProxy(proxyConfig.orElse(null));
        } else {
            LOGGER.info("proxyConfig was not found by proxyConfigCrn");
        }
    }

    public void executePostClusterManagerStartRecipes(Long stackId) throws CloudbreakException {
        recipeEngine.executePostAmbariStartRecipes(
                stackService.getByIdWithListsInTransaction(stackId),
                hostGroupService.getRecipesByCluster(
                        stackService.getByIdWithListsInTransaction(stackId).getCluster().getId()));
    }

    public void setupMonitoring(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);

        if (componentConfigProviderService.getTelemetry(stackId).isMonitoringFeatureEnabled()) {
            clusterApiConnectors.getConnector(stack)
                    .clusterSecurityService()
                    .setupMonitoringUser();
            clusterMonitoringEngine.installAndStartMonitoring(
                    stack,
                    componentConfigProviderService.getTelemetry(stackId));
        }
    }

    public void prepareExtendedTemplate(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(cluster.getId());

        setInitialBlueprintText(cluster);

        String template = getClusterSetupService(stack)
                .prepareTemplate(
                        loadInstanceMetadataForHostGroups(hostGroups),
                        conversionService.convert(stack, TemplatePreparationObject.class),
                        getSdxContext(stack),
                        getSdxStackCrn(stack),
                        kerberosConfigService.get(
                                stack.getEnvironmentCrn(),
                                stack.getName()
                        ).orElse(null)
                );

        validateExtendedBlueprintTextDoesNotContainUnresolvedHandlebarParams(template);

        cluster.setExtendedBlueprintText(template);
        clusterService.save(cluster);
    }

    public void finalizeClusterInstall(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());

        try {
            transactionService.required(() -> {
                Set<InstanceMetaData> instanceMetaDatas = loadInstanceMetadataForHostGroups(hostGroups).values()
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                finalizeClusterInstallHandlerService.finalizeClusterInstall(instanceMetaDatas, stack.getCluster());
            });
        } catch (TransactionExecutionException e) {
            throw new CloudbreakException(e.getCause());
        }
    }

    public void executePostInstallRecipes(Long stackId) throws CloudbreakException {
        recipeEngine.executePostInstallRecipes(
                stackService.getByIdWithListsInTransaction(stackId));
    }

    private String getSdxStackCrn(Stack stack) {
        return getSdxStack(stack)
                .map(Stack::getResourceCrn)
                .orElse(null);
    }

    private String getSdxContext(Stack stack) {
        return getSdxStack(stack)
                .map(clusterApiConnectors::getConnector)
                .map(ClusterApi::getSdxContext)
                .orElse(null);
    }

    private Optional<Stack> getSdxStack(Stack stack) {
        return datalakeService.getDatalakeStackByStackEnvironmentCrn(stack);
    }

    private ClusterSetupService getClusterSetupService(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return getClusterSetupService(stack);
    }

    private ClusterSetupService getClusterSetupService(Stack stack) {
        return clusterApiConnectors.getConnector(stack)
                .clusterSetupService();
    }

    private void setInitialBlueprintText(Cluster cluster) {
        cluster.setExtendedBlueprintText(cluster.getBlueprint().getBlueprintText());
        clusterService.updateCluster(cluster);
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

    private void validateExtendedBlueprintTextDoesNotContainUnresolvedHandlebarParams(String template) {
        if (template.matches(".*\\{\\{\\{.*}}}.*")) {
            throw new IllegalStateException("Some of the template parameters has not been resolved! Please check your custom properties at cluster the " +
                    "cluster creation to be able to resolve them!");
        }
    }

}
