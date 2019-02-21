package com.sequenceiq.cloudbreak.core.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.clusterdefinition.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.sharedservice.AmbariDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
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
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterCreationSuccessHandler clusterCreationSuccessHandler;

    @Inject
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public void startCluster(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        connector.waitForServer(stack);
        connector.changeOriginalCredentialsAndCreateCloudbreakUser();
    }

    public void buildCluster(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
        Cluster cluster = stack.getCluster();
        clusterService.updateCreationDateOnCluster(cluster);
        TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = loadInstanceMetadataForHostGroups(hostGroups);
        recipeEngine.executePostAmbariStartRecipes(stack, instanceMetaDataByHostGroup.keySet());
        String clusterDefinitionText = cluster.getClusterDefinition().getClusterDefinitionText();
        cluster.setExtendedClusterDefinitionText(clusterDefinitionText);
        clusterService.updateCluster(cluster);
        clusterService.save(connector.buildCluster(instanceMetaDataByHostGroup, templatePreparationObject, hostsInCluster));
        recipeEngine.executePostInstallRecipes(stack, instanceMetaDataByHostGroup.keySet());
        configureSmartsense(stack, connector);
        clusterCreationSuccessHandler.handleClusterCreationSuccess(stack);
        if (StackType.DATALAKE == stack.getType()) {
            try {
                transactionService.required(() -> {
                    Stack stackInTransaction = stackService.getByIdWithListsInTransaction(stackId);
                    ambariDatalakeConfigProvider.collectAndStoreDatalakeResources(stackInTransaction);
                    return null;
                });
            } catch (TransactionService.TransactionExecutionException e) {
                LOGGER.info("Couldn't collect Datalake paramaters", e);
            }
        }
    }

    private Map<HostGroup, List<InstanceMetaData>> loadInstanceMetadataForHostGroups(Iterable<HostGroup> hostGroups) {
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
            List<InstanceMetaData> metas = instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroupId);
            instanceMetaDataByHostGroup.put(hostGroup, metas);
        }
        return instanceMetaDataByHostGroup;
    }

    private void configureSmartsense(Stack stack, ClusterApi connector) {
        Optional<SmartSenseSubscription> smartSenseSubscription = smartSenseSubscriptionService.getDefault();
        if (smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)) {
            connector.clusterSetupService().configureSmartSense();
        }
    }
}
