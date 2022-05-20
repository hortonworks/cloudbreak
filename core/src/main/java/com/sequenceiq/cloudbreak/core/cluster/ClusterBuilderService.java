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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.saas.sdx.PaasRemoteDataContextSupplier;
import com.sequenceiq.cloudbreak.saas.sdx.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.FinalizeClusterInstallHandlerService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterBuilderService implements PaasRemoteDataContextSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBuilderService.class);

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
    private KerberosConfigService kerberosConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

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

        getClusterSetupService(stack).configureManagementServices(
                stackToTemplatePreparationObjectConverter.convert(stack),
                getSdxContextOptional(stack.getDatalakeCrn()).orElse(null),
                stack.getDatalakeCrn(),
                componentConfigProviderService.getTelemetry(stackId),
                proxyConfig.orElse(null));
    }

    public void configureSupportTags(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        getClusterSetupService(stack).configureSupportTags(stackToTemplatePreparationObjectConverter.convert(stack));
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
                hostGroupService.getByClusterWithRecipes(
                        stackService.getByIdWithListsInTransaction(stackId).getCluster().getId()));
    }

    public void prepareExtendedTemplate(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(cluster.getId());

        setInitialBlueprintText(cluster);

        String template = getClusterSetupService(stack)
                .prepareTemplate(
                        loadInstanceMetadataForHostGroups(hostGroups),
                        stackToTemplatePreparationObjectConverter.convert(stack),
                        getSdxContextOptional(stack.getDatalakeCrn()).orElse(null),
                        stack.getDatalakeCrn(),
                        kerberosConfigService.get(
                                stack.getEnvironmentCrn(),
                                stack.getName()
                        ).orElse(null)
                );

        validateExtendedBlueprintTextDoesNotContainUnresolvedHandlebarParams(template);

        cluster.setExtendedBlueprintText(template);
        clusterService.save(cluster);
    }

    public void finalizeClusterInstall(Stack stack) throws CloudbreakException {
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
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        if (stackView.getClusterView() != null) {
            recipeEngine.executePostInstallRecipes(
                    stackService.getByIdWithListsInTransaction(stackId), hostGroupService.getByClusterWithRecipes(stackView.getClusterView().getId()));
        }
    }

    public Optional<String> getSdxContextOptional(String sdxCrn) {
        if (StringUtils.isNotBlank(sdxCrn)) {
            return platformAwareSdxConnector.getRemoteDataContext(sdxCrn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getPaasSdxRemoteDataContext(String sdxCrn) {
        return Optional.ofNullable(stackService.getByCrnOrElseNull(sdxCrn))
                .map(clusterApiConnectors::getConnector)
                .map(ClusterApi::getSdxContext);
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
