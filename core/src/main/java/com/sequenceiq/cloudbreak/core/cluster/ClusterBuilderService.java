package com.sequenceiq.cloudbreak.core.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
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
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.sharedservice.AmbariDatalakeConfigProvider;
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
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

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
    private HostMetadataService hostMetadataService;

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
        connector.waitForServer(stack);
        boolean ldapConfigured = ldapConfigService.isLdapConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName());
        connector.changeOriginalCredentialsAndCreateCloudbreakUser(ldapConfigured);
    }

    public void buildCluster(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipesAndHostmetadata(stack.getCluster().getId());
        Cluster cluster = stack.getCluster();
        clusterService.updateCreationDateOnCluster(cluster);
        TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
        Set<HostMetadata> hostsInCluster = hostMetadataService.findHostsInCluster(cluster.getId());
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = loadInstanceMetadataForHostGroups(hostGroups);
        recipeEngine.executePostAmbariStartRecipes(stack, instanceMetaDataByHostGroup.keySet());
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        cluster.setExtendedBlueprintText(blueprintText);
        clusterService.updateCluster(cluster);
        final Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);

        if (cluster.getProxyConfigCrn() != null) {
            ProxyConfig proxyConfig = proxyConfigDtoService.getByCrn(cluster.getProxyConfigCrn());
            if (proxyConfig != null) {
                LOGGER.info("proxyConfig is not null, setup proxy for cluster");
                connector.clusterSetupService().setupProxy(proxyConfig);
            } else {
                LOGGER.info("proxyConfig was not found by proxyConfigCrn");
            }
        }

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

        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        clusterService.save(connector.clusterSetupService().buildCluster(
                instanceMetaDataByHostGroup, templatePreparationObject, hostsInCluster, sdxContext, sdxStackCrn, telemetry, kerberosConfig));
        recipeEngine.executePostInstallRecipes(stack, instanceMetaDataByHostGroup.keySet());
        clusterCreationSuccessHandler.handleClusterCreationSuccess(stack);
        if (StackType.DATALAKE == stack.getType()) {
            try {
                transactionService.required(() -> {
                    Stack stackInTransaction = stackService.getByIdWithListsInTransaction(stackId);
                    if (blueprintUtils.isAmbariBlueprint(blueprintText)) {
                        ambariDatalakeConfigProvider.collectAndStoreDatalakeResources(stackInTransaction);
                    } else {
                        clouderaManagerDatalakeConfigProvider.collectAndStoreDatalakeResources(stackInTransaction);
                    }
                    return null;
                });
            } catch (TransactionExecutionException e) {
                LOGGER.info("Couldn't collect Datalake paramaters", e);
            }
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
