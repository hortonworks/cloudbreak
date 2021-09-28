package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private KerberosConfigService kerberosConfigService;

    public void upscaleClusterManager(Long stackId, String hostGroupName, Integer scalingAdjustment, boolean primaryGatewayChanged)
            throws CloudbreakException, ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start adding cluster containers");
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();

        Map<String, String> hosts = hostRunner.addClusterServices(stackId, hostGroupName, scalingAdjustment);
        if (primaryGatewayChanged) {
            clusterServiceRunner.updateAmbariClientConfig(stack, stack.getCluster());
        }
        for (String hostName : hosts.keySet()) {
            if (!hostsPerHostGroup.containsKey(hostGroupName)) {
                hostsPerHostGroup.put(hostGroupName, new ArrayList<>());
            }
            hostsPerHostGroup.get(hostGroupName).add(hostName);
        }
        clusterService.updateInstancesToRunning(stack.getCluster().getId(), hostsPerHostGroup);

        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        connector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId).getRunningInstanceMetaDataSet());
    }

    public void uploadRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start executing pre recipes");
        HostGroup hostGroup = Optional.ofNullable(hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName))
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());
        recipeEngine.uploadUpscaleRecipes(stack, hostGroup, hostGroups);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName, Boolean repair, Boolean restartServices) throws CloudbreakException {
        Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
        LOGGER.debug("Start installing CM services");
        HostGroup hostGroup = Optional.ofNullable(hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName))
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        Set<InstanceMetaData> runningInstanceMetaDataSet = hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet();
        recipeEngine.executePostAmbariStartRecipes(stack, Set.of(hostGroup));
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        List<String> upscaledHosts = connector.upscaleCluster(hostGroup, runningInstanceMetaDataSet);
        if (shouldRestartServices(repair, restartServices, stack)) {
            try {
                LOGGER.info("Trying to restart services");
                connector.restartAll(false);
            } catch (RuntimeException e) {
                LOGGER.info("Restart services failed", e);
            }
        }
        runningInstanceMetaDataSet.stream()
                .filter(instanceMetaData -> upscaledHosts.contains(instanceMetaData.getDiscoveryFQDN()))
                .forEach(instanceMetaData -> {
                    instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
                    instanceMetaDataService.save(instanceMetaData);
                });
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
