package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_UPSCALE_FAILED;
import static java.lang.String.format;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ScalingException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleService.class);

    @Inject
    private StackDtoService stackDtoService;

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
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ParcelService parcelService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private EntitlementService entitlementService;

    public void installServicesOnNewHosts(Long stackId, Set<String> hostGroupNames, Boolean repair, Boolean restartServices,
            Map<String, Set<String>> hostGroupsWithHostNames, Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged)
            throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.debug("Start installing CM services");
        removeUnusedParcelComponents(stackDto);
        Set<HostGroup> hostGroupSetWithRecipes = hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId());
        Set<HostGroup> hostGroupSetWithInstanceMetadas = hostGroupService.getByCluster(stackDto.getCluster().getId());
        Map<HostGroup, Set<InstanceMetaData>> instanceMetaDatasByHostGroup = hostGroupSetWithInstanceMetadas.stream()
                .filter(hostGroup -> hostGroupNames.contains(hostGroup.getName()))
                .collect(Collectors.toMap(Function.identity(), hostGroup -> hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet()));
        Map<String, String> candidateAddresses = clusterHostServiceRunner.collectUpscaleCandidates(stackDto, hostGroupWithAdjustment, false);
        recipeEngine.executePostClouderaManagerStartRecipesOnTargets(stackDto, hostGroupSetWithRecipes, candidateAddresses);
        Set<InstanceMetaData> runningInstanceMetaDataSet =
                hostGroupSetWithInstanceMetadas.stream()
                        .flatMap(hostGroup -> hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet().stream())
                        .collect(Collectors.toSet());
        ClusterApi connector = getClusterConnector(stackDto);
        try {
            List<String> upscaledHosts = connector.upscaleCluster(instanceMetaDatasByHostGroup);
            if (Boolean.TRUE.equals(repair)) {
                recommissionHostsIfNeeded(connector, hostGroupsWithHostNames);
                restartServicesIfNecessary(restartServices, stackDto, connector);
                connector.hostsStartRoles(upscaledHosts);
            }
            clusterHostServiceRunner.createCronForUserHomeCreation(stackDto, candidateAddresses.keySet());
            setInstanceStatus(runningInstanceMetaDataSet, upscaledHosts);
        } catch (ScalingException se) {
            flowMessageService.fireEventAndLog(stackDto.getId(), Status.UPDATE_IN_PROGRESS.name(),
                    CLUSTER_SCALING_UPSCALE_FAILED, Joiner.on(",").join(se.getFailedInstanceIds()));
            if (Boolean.FALSE.equals(repair) && !primaryGatewayChanged && entitlementService.targetedUpscaleSupported(stackDto.getAccountId())) {
                clusterService.updateInstancesToZombieByInstanceIds(stackId, se.getFailedInstanceIds());
            } else {
                clusterService.updateInstancesToOrchestrationFailedByInstanceIds(stackId, se.getFailedInstanceIds());
                throw se;
            }
        }
    }

    private void removeUnusedParcelComponents(StackDto stackDto) throws CloudbreakException {
        ParcelOperationStatus parcelOperationStatus = parcelService.removeUnusedParcelComponents(stackDto);
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

    private void restartServicesIfNecessary(Boolean restartServices, StackDto stackDto, ClusterApi connector) throws CloudbreakException {
        if (shouldRestartServices(restartServices, stackDto)) {
            try {
                LOGGER.info("Trying to restart services");
                connector.restartAll(false);
            } catch (RuntimeException e) {
                LOGGER.info("Restart services failed", e);
            }
        }
    }

    private boolean shouldRestartServices(Boolean restartServices, StackDto stackDto) {
        return restartServices && stackDto.getAllAvailableInstances().size() == stackDto.getRunningInstanceMetaDataSet().size();
    }

    public void executePostRecipesOnNewHosts(Long stackId, Map<String, Integer> hostGroupWithAdjustment) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.debug("Start executing post recipes");
        recipeEngine.executePostServiceDeploymentRecipes(stackDto, hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId()),
                clusterHostServiceRunner.collectUpscaleCandidates(stackDto, hostGroupWithAdjustment, false));
    }

    public Map<String, String> gatherInstalledComponents(Long stackId, String hostname) {
        // TODO: Dead code, will be removed with https://jira.cloudera.com/browse/CB-8349
        return Map.of();
    }

    public void ensureComponentsAreStopped(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Ensuring components are in stopped state in ambari on host {}", hostname);
        getClusterConnector(stackDto).ensureComponentsAreStopped(components, hostname);
    }

    public void initComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Start init components in ambari on host {}", hostname);
        getClusterConnector(stackDto).initComponents(components, hostname);
    }

    public void stopComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Start stop components in ambari on host {}", hostname);
        getClusterConnector(stackDto).stopComponents(components, hostname);
    }

    public void installComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Start installing components in ambari on host {}", hostname);
        getClusterConnector(stackDto).installComponents(components, hostname);
    }

    public void regenerateKerberosKeytabs(Long stackId, String hostname) {
        StackDto stackDto = stackDtoService.getById(stackId);
        StackView stack = stackDto.getStack();
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        LOGGER.info("Start regenerate kerberos keytabs in ambari on host {}", hostname);
        getClusterConnector(stackDto).clusterModificationService().regenerateKerberosKeytabs(hostname, kerberosConfig);
    }

    public void startComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Start components in ambari on host {}", hostname);
        getClusterConnector(stackDto).startComponents(components, hostname);
    }

    private void recommissionHostsIfNeeded(ClusterApi connector, Map<String, Set<String>> hostGroupsWithHostNames) {
        Set<String> hosts = hostGroupsWithHostNames.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (!hosts.isEmpty()) {
            Set<String> decommissionedHosts = new HashSet<>(connector.clusterStatusService().getDecommissionedHostsFromCM());
            List<String> hostsToCommission = hosts.stream()
                    .filter(decommissionedHosts::contains)
                    .collect(Collectors.toList());
            if (!hostsToCommission.isEmpty()) {
                LOGGER.info("The following hosts will be recommissioned since they are decommissioned and selected as repairable hosts. Hosts: {}",
                        hostsToCommission);
                connector.clusterCommissionService().recommissionHosts(hostsToCommission);
            }
        }
    }

    public void restartAll(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("Restart all in ambari");
        getClusterConnector(stackDto).restartAll(false);
    }

    private ClusterApi getClusterConnector(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto);
    }
}
