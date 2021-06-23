package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.AttachedClusterInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class DatalakeService {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    public static final String RANGER_PASSWORD = "ranger.admin.password";

    public static final String RANGER_PORT = "ranger.service.http.port";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    private static final int FIRST = 0;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private TransactionService transactionService;

    public void prepareDatalakeRequest(Stack source, StackV4Request stackRequest) {
        if (!Strings.isNullOrEmpty(source.getDatalakeCrn())) {
            LOGGER.debug("Prepare datalake request by datalakecrn");
            SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
            Stack datalakeStack = stackService.getByCrn(source.getDatalakeCrn());
            sharedServiceRequest.setDatalakeName(datalakeStack.getName());
            stackRequest.setSharedService(sharedServiceRequest);
        } else if (source.getDatalakeResourceId() != null) {
            LOGGER.debug("Prepare datalake request by datalake resource id");
            Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findById(source.getDatalakeResourceId());
            if (datalakeResources.isPresent()) {
                SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
                sharedServiceRequest.setDatalakeName(datalakeResources.get().getName());
                stackRequest.setSharedService(sharedServiceRequest);
            }
        }
    }

    public void addSharedServiceResponse(ClusterApiView cluster, ClusterViewV4Response clusterResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (cluster.getStack().getDatalakeCrn() != null) {
            LOGGER.debug("Add shared service response by datalakeCrn");
            Stack datalakeStack = stackService.getByCrn(cluster.getStack().getDatalakeCrn());
            if (datalakeStack != null) {
                sharedServiceResponse.setSharedClusterId(datalakeStack.getId());
                sharedServiceResponse.setSharedClusterName(datalakeStack.getName());
            }
        } else if (cluster.getStack().getDatalakeId() != null) {
            LOGGER.debug("Add shared service response by datalake resource");
            Optional<DatalakeResources> datalakeResource = datalakeResourcesService.findById(cluster.getStack().getDatalakeId());
            if (datalakeResource.isPresent()) {
                sharedServiceResponse.setSharedClusterId(datalakeResource.get().getDatalakeStackId());
                sharedServiceResponse.setSharedClusterName(datalakeResource.get().getName());
            }
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    public void addSharedServiceResponse(Stack stack, StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (!Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
            setSharedServiceResponseByDatalakeCrn(stack, sharedServiceResponse);
        } else if (stack.getDatalakeResourceId() != null) {
            setSharedServiceResponseByDatalakeResource(stack, sharedServiceResponse);
        } else {
            setSharedServiceResponseWithoutConnection(stack, sharedServiceResponse);
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

    private void setSharedServiceResponseWithoutConnection(Stack stack, SharedServiceV4Response sharedServiceResponse) {
        LOGGER.debug("Datalake resource ID was null! Going to search it by the given stack's id (id: {})", stack.getId());
        Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findByDatalakeStackId(stack.getId());
        if (datalakeResources.isPresent()) {
            LOGGER.debug("DatalakeResources (datalake stack id: {} ,name: {}) has found for stack: {}",
                    datalakeResources.get().getDatalakeStackId(), datalakeResources.get().getName(), stack.getResourceCrn());
            for (StackIdView connectedStacks : stackService.findClustersConnectedToDatalakeByDatalakeResourceId(datalakeResources.get().getId())) {
                AttachedClusterInfoV4Response attachedClusterInfoResponse = new AttachedClusterInfoV4Response();
                attachedClusterInfoResponse.setId(connectedStacks.getId());
                attachedClusterInfoResponse.setName(connectedStacks.getName());
                sharedServiceResponse.getAttachedClusters().add(attachedClusterInfoResponse);
            }
        } else {
            LOGGER.debug("Unable to find DatalakeResources by the stack's id: {}", stack.getId());
        }
    }

    private void setSharedServiceResponseByDatalakeResource(Stack stack, SharedServiceV4Response sharedServiceResponse) {
        LOGGER.debug("Checking whether the datalake resource id is null or not for the cluster: " + stack.getResourceCrn());
        Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findById(stack.getDatalakeResourceId());
        if (datalakeResources.isPresent()) {
            DatalakeResources datalakeResource = datalakeResources.get();
            sharedServiceResponse.setSharedClusterId(datalakeResource.getDatalakeStackId());
            sharedServiceResponse.setSharedClusterName(datalakeResource.getName());
            LOGGER.debug("DatalakeResources (datalake stack id: {} ,name: {}) has found for stack: {}",
                    datalakeResource.getDatalakeStackId(), datalakeResource.getName(), stack.getResourceCrn());
        } else {
            LOGGER.debug("Unable to find DatalakeResources for datalake resource id: " + stack.getDatalakeResourceId());
        }
    }

    private void setSharedServiceResponseByDatalakeCrn(Stack stack, SharedServiceV4Response sharedServiceResponse) {
        LOGGER.debug("Checking datalake through the datalakeCrn.");
        Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
        if (datalakeStack != null) {
            LOGGER.debug("Datalake (stack id {}, name {}) has been found for stack: {}",
                    datalakeStack.getId(), datalakeStack.getName(), stack.getResourceCrn());
            sharedServiceResponse.setSharedClusterName(datalakeStack.getName());
            sharedServiceResponse.setSharedClusterId(datalakeStack.getId());
        }
    }

    public Optional<Stack> getDatalakeStackByDatahubStack(Stack datahubStack) {
        if (!Strings.isNullOrEmpty(datahubStack.getDatalakeCrn())) {
            LOGGER.debug("Get datalake by datahub's datalakeCrn");
            return Optional.of(stackService.getByCrn(datahubStack.getDatalakeCrn()));
        }

        LOGGER.debug("Get datalake by datahub's datalake resource");
        Long datalakeResourceId = datahubStack.getDatalakeResourceId();
        if (datalakeResourceId != null) {
            Optional<DatalakeResources> datalakeResource = datalakeResourcesService.findById(datalakeResourceId);
            if (datalakeResource.isPresent() && datalakeResource.get().getDatalakeStackId() != null) {
                Long datalakeStackId = datalakeResource.get().getDatalakeStackId();
                Stack datalakeStack = stackService.getByIdWithListsInTransaction(datalakeStackId);
                return Optional.of(datalakeStack);
            }
        }
        LOGGER.info("There is no datalake resource nor datalakecrn has set for the cluster.");
        return Optional.empty();
    }

    public Optional<Stack> getDatalakeStackByStackEnvironmentCrn(Stack datahubStack) {
        if (StackType.DATALAKE.equals(datahubStack.getType())) {
            return Optional.empty();
        }
        List<StackStatusView> res = stackService.getByEnvironmentCrnAndStackType(datahubStack.getEnvironmentCrn(), StackType.DATALAKE);
        if (res.isEmpty()) {
            return Optional.empty();
        } else {
            // it has the assumption that environemt and datalake has 1:1 connection
            return Optional.of(stackService.getByIdWithListsInTransaction(res.get(FIRST).getId()));
        }
    }

    public Optional<Long> getDatalakeResourceId(Long datalakeStackId) {
        Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findByDatalakeStackId(datalakeStackId);
        if (datalakeResources.isPresent()) {
            return Optional.of(datalakeResources.get().getId());
        }
        return Optional.empty();
    }

    public SharedServiceConfigsView createSharedServiceConfigsView(Stack source) {
        Cluster cluster = source.getCluster();
        if (!Strings.isNullOrEmpty(source.getDatalakeCrn())) {
            return createSharedServiceConfigView(source);
        }
        Optional<DatalakeResources> datalakeResources = getDataLakeResource(source);
        return createSharedServiceConfigsView(cluster.getBlueprint(), cluster.getPassword(), datalakeResources);
    }

    public String getDatalakeCrn(StackV4Request source, Workspace workspace) {
        if (source.getSharedService() != null && isNotBlank(source.getSharedService().getDatalakeName())) {
            Optional<Stack> result = stackService
                    .findStackByNameOrCrnAndWorkspaceId(NameOrCrn.ofName(source.getSharedService().getDatalakeName()), workspace.getId());
            if (result.isPresent()) {
                return result.get().getResourceCrn();
            } else {
                LOGGER.debug("No datalake resource found for data lake: {}", source.getSharedService().getDatalakeName());
            }
        }
        return null;
    }

    public void setDatalakeIdOnStack(Stack stack, StackV4Request source, Workspace workspace) {
        Optional<DatalakeResources> datalakeResources = getDatalakeResourceId(source, workspace);
        if (datalakeResources.isPresent()) {
            stack.setDatalakeResourceId(datalakeResources.get().getId());
            stack.setDatalakeCrn(stackService.get(datalakeResources.get().getDatalakeStackId()).getResourceCrn());
        } else {
            stack.setDatalakeResourceId(null);
        }
    }

    private Optional<DatalakeResources> getDataLakeResource(Stack source) {
        if (source.getDatalakeResourceId() != null) {
            return datalakeResourcesService.findById(source.getDatalakeResourceId());
        }
        return Optional.empty();
    }

    private Optional<String> getHostForComponentInService(DatalakeResources datalakeResources, String service, String serviceComponent) {
        if (datalakeResources.getServiceDescriptorMap().containsKey(service)) {
            ServiceDescriptor serviceDescriptor = datalakeResources.getServiceDescriptorMap().get(service);
            Map<String, Object> componentHostMap = serviceDescriptor.getComponentsHosts().getMap();
            return Optional.of(String.valueOf(componentHostMap.get(serviceComponent)));
        } else {
            return Optional.empty();
        }
    }

    private String getRangerPort(Map<String, ServiceDescriptor> serviceDescriptorMap, String defaultPort) {
        if (serviceDescriptorMap.containsKey(RANGER)) {
            ServiceDescriptor serviceDescriptor = serviceDescriptorMap.get(RANGER);
            Map<String, Object> params = serviceDescriptor.getBlueprintParams().getMap();
            return String.valueOf(params.getOrDefault(RANGER_PORT, defaultPort));
        }
        return defaultPort;
    }

    private Optional<String> getServiceRelatedSecret(Map<String, ServiceDescriptor> serviceDescriptorMap, String service, String secretKey) {
        if (serviceDescriptorMap.containsKey(service)) {
            ServiceDescriptor serviceDescriptor = serviceDescriptorMap.get(service);
            Map<String, Object> secretMap = serviceDescriptor.getBlueprintSecretParams().getMap();
            if (secretMap.containsKey(secretKey)) {
                return Optional.of(String.valueOf(secretMap.get(secretKey)));
            }
        }
        return Optional.empty();
    }

    private Optional<String> getServiceRelatedSecret(DatalakeResources datalakeResources, String service, String secretKey) {
        return getServiceRelatedSecret(datalakeResources.getServiceDescriptorMap(), service, secretKey);
    }

    private Optional<String> getRangerAdminPassword(DatalakeResources datalakeResources) {
        return getServiceRelatedSecret(datalakeResources, RANGER, RANGER_PASSWORD);
    }

    private Optional<DatalakeResources> getDatalakeResourceId(StackV4Request source, Workspace workspace) {
        try {
            if (source.getSharedService() != null && isNotBlank(source.getSharedService().getDatalakeName())) {
                return Optional.of(datalakeResourcesService.getByNameForWorkspace(source.getSharedService().getDatalakeName(), workspace));
            } else {
                LOGGER.debug("No datalake resource because shared service or datalake name is not filled");
            }
        } catch (NotFoundException nfe) {
            LOGGER.debug("No datalake resource found for data lake: {}", source.getSharedService().getDatalakeName());
        }
        return Optional.empty();
    }

    private SharedServiceConfigsView createSharedServiceConfigsView(Blueprint blueprint, String ambariPassword,
            Optional<DatalakeResources> datalakeResources) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (datalakeResources.isPresent()) {
            DatalakeResources datalakeResource = datalakeResources.get();
            String rangerPort = getRangerPort(datalakeResource.getServiceDescriptorMap(), DEFAULT_RANGER_PORT);
            sharedServiceConfigsView.setRangerAdminPassword(getRangerAdminPassword(datalakeResource).orElse(null));
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setDatalakeClusterManagerIp(datalakeResource.getDatalakeAmbariIp());
            sharedServiceConfigsView.setDatalakeClusterManagerFqdn(datalakeResource.getDatalakeAmbariFqdn());
            sharedServiceConfigsView.setDatalakeComponents(datalakeResource.getDatalakeComponentSet());
            sharedServiceConfigsView.setRangerAdminPort(rangerPort);
            Optional<String> rangerAdminHost = getHostForComponentInService(datalakeResource, RANGER, RANGER_ADMIN);
            sharedServiceConfigsView.setRangerAdminHost(rangerAdminHost.orElse(null));
        } else if (blueprintUtils.isSharedServiceReadyBlueprint(blueprint)) {
            setRangerAttributes(ambariPassword, sharedServiceConfigsView);
            sharedServiceConfigsView.setDatalakeCluster(true);
        } else {
            setRangerAttributes(ambariPassword, sharedServiceConfigsView);
            sharedServiceConfigsView.setDatalakeCluster(false);
        }

        return sharedServiceConfigsView;
    }

    private void setRangerAttributes(String ambariPassword, SharedServiceConfigsView sharedServiceConfigsView) {
        sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
        sharedServiceConfigsView.setAttachedCluster(false);
        sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
    }

    private SharedServiceConfigsView createSharedServiceConfigView(Stack stack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (!Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
            Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
            if (datalakeStack != null) {
                sharedServiceConfigsView.setDatalakeClusterManagerFqdn(getDatalakeClusterManagerFqdn(datalakeStack));
                sharedServiceConfigsView.setDatalakeClusterManagerIp(datalakeStack.getClusterManagerIp());
            }
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setAttachedCluster(true);
        }

        return sharedServiceConfigsView;
    }

    private String getDatalakeClusterManagerFqdn(Stack datalakeStack) {
        String fqdn;
        try {
            fqdn = transactionService.required(() -> datalakeStack.getGatewayInstanceMetadata().isEmpty()
                    ? datalakeStack.getClusterManagerIp() : datalakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN());
        } catch (Exception ignored) {
            LOGGER.debug("Get instance metadata transaction failed, giving back IP as FQDN");
            fqdn = datalakeStack.getClusterManagerIp();
        }
        return fqdn;
    }
}
