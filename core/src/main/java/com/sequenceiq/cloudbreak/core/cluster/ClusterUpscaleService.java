package com.sequenceiq.cloudbreak.core.cluster;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private ParcelService parcelService;

    public void uploadRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start executing pre recipes");
        HostGroup hostGroup = getHostGroup(hostGroupName, stack);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());
        recipeEngine.uploadUpscaleRecipes(stack, hostGroup, hostGroups);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName, Boolean repair, Boolean restartServices) throws CloudbreakException {
        Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
        LOGGER.debug("Start installing CM services");
        removeUnusedParcelComponents(stack);
        HostGroup hostGroup = getHostGroup(hostGroupName, stack);
        recipeEngine.executePostAmbariStartRecipes(stack, Set.of(hostGroup));
        Set<InstanceMetaData> runningInstanceMetaDataSet = hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet();
        ClusterApi connector = getClusterConnector(stack);
        List<String> upscaledHosts = connector.upscaleCluster(hostGroup, runningInstanceMetaDataSet);
        restartServicesIfNecessary(repair, restartServices, stack, connector);
        setInstanceStatus(runningInstanceMetaDataSet, upscaledHosts);
    }

    private void removeUnusedParcelComponents(Stack stack) throws CloudbreakException {
        ParcelOperationStatus parcelOperationStatus = parcelService.removeUnusedParcelComponents(stack);
        if (!parcelOperationStatus.getFailed().isEmpty()) {
            LOGGER.error("There are failed parcel removals: {}", parcelOperationStatus);
            throw new CloudbreakException(format("Failed to remove the following parcels: %s", parcelOperationStatus.getFailed()));
        }
    }

    private void setInstanceStatus(Set<InstanceMetaData> runningInstanceMetaDataSet, List<String> upscaledHosts) {
        runningInstanceMetaDataSet.stream()
                .filter(instanceMetaData -> upscaledHosts.contains(instanceMetaData.getDiscoveryFQDN()))
                .forEach(instanceMetaData -> {
                    instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
                    instanceMetaDataService.save(instanceMetaData);
                });
    }

    private HostGroup getHostGroup(String hostGroupName, Stack stack) {
        return Optional.ofNullable(hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName))
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
    }

    private void restartServicesIfNecessary(Boolean repair, Boolean restartServices, Stack stack, ClusterApi connector) throws CloudbreakException {
        if (shouldRestartServices(repair, restartServices, stack)) {
            try {
                LOGGER.info("Trying to restart services");
                connector.restartAll(false);
            } catch (RuntimeException e) {
                LOGGER.info("Restart services failed", e);
            }
        }
    }

    private boolean shouldRestartServices(Boolean repair, Boolean restartServices, Stack stack) {
        return repair && restartServices && stack.getNotTerminatedInstanceMetaDataList().size() == stack.getRunningInstanceMetaDataSet().size();
    }

    public void executePostRecipesOnNewHosts(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start executing post recipes");
        recipeEngine.executePostInstallRecipes(stack, hostGroupService.getByClusterWithRecipes(stack.getCluster().getId()));
    }

    public Map<String, String> gatherInstalledComponents(Long stackId, String hostname) {
        // TODO: Dead code, will be removed with https://jira.cloudera.com/browse/CB-8349
        return Map.of();
    }

    public void ensureComponentsAreStopped(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Ensuring components are in stopped state in ambari on host {}", hostname);
        getClusterConnector(stack).ensureComponentsAreStopped(components, hostname);
    }

    public void initComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start init components in ambari on host {}", hostname);
        getClusterConnector(stack).initComponents(components, hostname);
    }

    public void stopComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start stop components in ambari on host {}", hostname);
        getClusterConnector(stack).stopComponents(components, hostname);
    }

    public void installComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start installing components in ambari on host {}", hostname);
        getClusterConnector(stack).installComponents(components, hostname);
    }

    public void regenerateKerberosKeytabs(Long stackId, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        LOGGER.info("Start regenerate kerberos keytabs in ambari on host {}", hostname);
        getClusterConnector(stack).clusterModificationService().regenerateKerberosKeytabs(hostname, kerberosConfig);
    }

    public void startComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start components in ambari on host {}", hostname);
        getClusterConnector(stack).startComponents(components, hostname);
    }

    public void restartAll(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Restart all in ambari");
        getClusterConnector(stack).restartAll(false);
    }

    private ClusterApi getClusterConnector(Stack stack) {
        return clusterApiConnectors.getConnector(stack);
    }
}
