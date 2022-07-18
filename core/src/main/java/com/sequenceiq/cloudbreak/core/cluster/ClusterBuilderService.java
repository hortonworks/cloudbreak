package com.sequenceiq.cloudbreak.core.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterBuilderService implements PaasRemoteDataContextSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBuilderService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

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
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public void startCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        connector.waitForServer(true);
        boolean ldapConfigured = ldapConfigService.isLdapConfigExistsForEnvironment(stackDto.getStack().getEnvironmentCrn(), stackDto.getStack().getName());
        connector.changeOriginalCredentialsAndCreateCloudbreakUser(ldapConfigured);
    }

    public void waitForClusterManager(Long stackId) throws CloudbreakException, ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);
        clusterService.updateCreationDateOnCluster(stackDto.getCluster().getId());
        clusterApiConnectors
                .getConnector(stackDto)
                .waitForServer(true);
    }

    public void validateLicence(Long stackId) {
        getClusterSetupService(stackId).validateLicence();
    }

    public void configureManagementServices(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(
                stackDto.getCluster().getProxyConfigCrn(),
                stackDto.getCluster().getEnvironmentCrn());

        getClusterSetupService(stackDto).configureManagementServices(
                stackToTemplatePreparationObjectConverter.convert(stackDto),
                getSdxContextOptional(stackDto.getDatalakeCrn()).orElse(null),
                stackDto.getDatalakeCrn(),
                componentConfigProviderService.getTelemetry(stackId),
                proxyConfig.orElse(null));
    }

    public void configureSupportTags(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
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
        StackDto stackDto = stackDtoService.getById(stackId);
        StackView stack = stackDto.getStack();
        getClusterSetupService(stackDto).configureKerberos(kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null));
    }

    public void installCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);
        getClusterSetupService(stackDto).installCluster(stackDto.getCluster().getExtendedBlueprintText());
    }

    public void autoConfigureCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);
        getClusterSetupService(stackDto).autoConfigureClusterManager();
    }

    public void prepareProxyConfig(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);

        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(
                stackDto.getCluster().getProxyConfigCrn(),
                stackDto.getCluster().getEnvironmentCrn());
        if (proxyConfig.isPresent()) {
            LOGGER.info("proxyConfig is not null, setup proxy for cluster: {}", proxyConfig);
            getClusterSetupService(stackDto).setupProxy(proxyConfig.orElse(null));
        } else {
            LOGGER.info("proxyConfig was not found by proxyConfigCrn");
        }
    }

    public void executePostClusterManagerStartRecipes(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        recipeEngine.executePostClouderaManagerStartRecipes(stackDto, hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId()));
    }

    public void prepareExtendedTemplate(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(cluster.getId());

        setInitialBlueprintText(stackDto);

        String template = getClusterSetupService(stackDto)
                .prepareTemplate(
                        loadInstanceMetadataForHostGroups(hostGroups),
                        stackToTemplatePreparationObjectConverter.convert(stackDto),
                        getSdxContextOptional(stackDto.getDatalakeCrn()).orElse(null),
                        stackDto.getDatalakeCrn(),
                        kerberosConfigService.get(
                                stack.getEnvironmentCrn(),
                                stack.getName()
                        ).orElse(null)
                );

        validateExtendedBlueprintTextDoesNotContainUnresolvedHandlebarParams(template);

        clusterService.updateExtendedBlueprintText(cluster.getId(), template);
    }

    public void finalizeClusterInstall(StackDto stackDto) throws CloudbreakException {
        List<InstanceMetadataView> instanceMetaDatas = stackDto.getAllAvailableInstances();
        finalizeClusterInstallHandlerService.finalizeClusterInstall(instanceMetaDatas, stackDto.getCluster());
    }

    public void executePostInstallRecipes(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (stackDto.getCluster() != null) {
            recipeEngine.executePostInstallRecipes(stackDto, hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId()));
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
        StackDto stack = stackDtoService.getById(stackId);
        return getClusterSetupService(stack);
    }

    private ClusterSetupService getClusterSetupService(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto)
                .clusterSetupService();
    }

    private void setInitialBlueprintText(StackDto stackDto) {
        clusterService.updateExtendedBlueprintText(stackDto.getCluster().getId(), stackDto.getBlueprint().getBlueprintText());
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
