package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CONFIGURE_POLICY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
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
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasRemoteDataContextSupplier;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
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
public class ClusterBuilderService implements LocalPaasRemoteDataContextSupplier {

    private static final Pattern HANDLEBAR_REGEX = Pattern.compile("\\{\\{\\{([a-zA-Z0-9. ]+)}}}");

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

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    public void startCluster(Long stackId) throws CloudbreakException, ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        connector.waitForServer(true);
        boolean ldapConfigured = ldapConfigService.isLdapConfigExistsForEnvironment(stackDto.getStack().getEnvironmentCrn(), stackDto.getStack().getName());
        connector.changeOriginalCredentialsAndCreateCloudbreakUser(ldapConfigured);
        if (StackType.DATALAKE.equals(stackDto.getType()) && stackDto.getAllPrimaryGatewayInstances().size() > 1) {
            connector.clusterModificationService().reconfigureCMMemory();
        }
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

        String datalakeCrn = getDatalakeCrn(stackDto);
        String sdxContext = getSdxContextOptional(datalakeCrn).orElse(null);
        if (StackType.DATALAKE.equals(stackDto.getStack().getType())) {
            datalakeCrn = stackDto.getResourceCrn();
        }

        getClusterSetupService(stackDto).configureManagementServices(
                stackToTemplatePreparationObjectConverter.convert(stackDto),
                sdxContext,
                datalakeCrn,
                componentConfigProviderService.getTelemetry(stackId),
                proxyConfig.orElse(null));
    }

    private String getDatalakeCrn(StackDto stackDto) {
        if (!stackDto.getStack().getType().equals(StackType.DATALAKE)) {
            if (StringUtils.isNotEmpty(stackDto.getDatalakeCrn())) {
                return stackDto.getDatalakeCrn();
            } else {
                LOGGER.info("Datalake CRN not found in Stack. Fetching the CRN SDX services.");
                return platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stackDto.getEnvironmentCrn()).map(SdxBasicView::crn).orElse(null);
            }
        }
        return stackDto.getDatalakeCrn();
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

    public void configurePolicy(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        boolean govCloud = stackDto.getPlatformVariant().equals(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        if (govCloud) {
            flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), CONFIGURE_POLICY);
            stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CONFIGURE_POLICY, "Configure FISMA Policies for Cloudera Manager");
        }
        getClusterSetupService(stackDto)
                .publishPolicy(stackDto.getCluster().getExtendedBlueprintText(), govCloud);
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
            getClusterSetupService(stackDto).setupProxy(proxyConfig.get());
        } else {
            LOGGER.info("proxyConfig was not found by proxyConfigCrn");
        }
    }

    public void modifyProxyConfig(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(
                stackDto.getCluster().getProxyConfigCrn(),
                stackDto.getCluster().getEnvironmentCrn());
        getClusterSetupService(stackDto).setupProxy(proxyConfig.orElse(null));
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
        String datalakeCrn = getDatalakeCrn(stackDto);
        String template = getClusterSetupService(stackDto)
                .prepareTemplate(
                        loadInstanceMetadataForHostGroups(hostGroups),
                        stackToTemplatePreparationObjectConverter.convert(stackDto),
                        getSdxContextOptional(datalakeCrn).orElse(null),
                        datalakeCrn,
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

    public void executePostServiceDeploymentRecipes(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        if (stackDto.getCluster() != null) {
            recipeEngine.executePostServiceDeploymentRecipes(stackDto, hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId()));
        }
    }

    public Optional<String> getSdxContextOptional(String sdxCrn) {
        if (StringUtils.isNotBlank(sdxCrn)) {
            return platformAwareSdxConnector.getRemoteDataContext(sdxCrn);
        }
        LOGGER.info("Skipping getSdxContextOptional as the Crn is empty");
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
        clusterService.updateExtendedBlueprintText(stackDto.getCluster().getId(), stackDto.getBlueprintJsonText());
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
        Matcher matcher = HANDLEBAR_REGEX.matcher(template);
        if (matcher.find()) {
            throw new IllegalStateException(String.format("Some of the template parameters has not been resolved!" +
                    " Please check your custom properties at cluster the cluster creation to be able to resolve them!" +
                    " Remaining handlebar value: {{{%s}}}", matcher.group(1)));
        }
    }
}
