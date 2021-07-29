package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class DatalakeService {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    private static final int FIRST = 0;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    public void prepareDatalakeRequest(Stack source, StackV4Request stackRequest) {
        if (!Strings.isNullOrEmpty(source.getDatalakeCrn())) {
            LOGGER.debug("Prepare datalake request by datalakecrn");
            SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
            Stack datalakeStack = stackService.getByCrn(source.getDatalakeCrn());
            sharedServiceRequest.setDatalakeName(datalakeStack.getName());
            stackRequest.setSharedService(sharedServiceRequest);
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
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    public void addSharedServiceResponse(Stack stack, StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (!Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
            LOGGER.debug("Checking datalake through the datalakeCrn.");
            Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
            if (datalakeStack != null) {
                LOGGER.debug("Datalake (stack id {}, name {}) has been found for stack: {}",
                        datalakeStack.getId(), datalakeStack.getName(), stack.getResourceCrn());
                sharedServiceResponse.setSharedClusterName(datalakeStack.getName());
                sharedServiceResponse.setSharedClusterId(datalakeStack.getId());
            }
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

    public Optional<Stack> getDatalakeStackByDatahubStack(Stack datahubStack) {
        if (!Strings.isNullOrEmpty(datahubStack.getDatalakeCrn())) {
            return Optional.of(stackService.getByCrn(datahubStack.getDatalakeCrn()));
        }
        LOGGER.info("There is no datalake has been set for the cluster.");
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
            // it has the assumption that environment and datalake has 1:1 connection
            return Optional.of(stackService.getByIdWithListsInTransaction(res.get(FIRST).getId()));
        }
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

    public SharedServiceConfigsView createSharedServiceConfigsView(Stack stack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        String password = stack.getCluster().getPassword();

        switch (stack.getType()) {
            case DATALAKE:
                setRangerAttributes(password, sharedServiceConfigsView);
                sharedServiceConfigsView.setDatalakeCluster(true);
                break;
            case WORKLOAD:
                sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
                sharedServiceConfigsView.setDatalakeCluster(false);
                sharedServiceConfigsView.setAttachedCluster(true);

                if (Strings.isNullOrEmpty(stack.getDatalakeCrn())) {
                    break;
                }
                Stack datalakeStack = stackService.getByCrn(stack.getDatalakeCrn());
                if (datalakeStack == null) {
                    break;
                }

                sharedServiceConfigsView.setDatalakeClusterManagerFqdn(getDatalakeClusterManagerFqdn(datalakeStack));
                sharedServiceConfigsView.setDatalakeClusterManagerIp(datalakeStack.getClusterManagerIp());
                break;
            default:
                setRangerAttributes(password, sharedServiceConfigsView);
                sharedServiceConfigsView.setDatalakeCluster(false);
                break;
        }

        return sharedServiceConfigsView;
    }

    private void setRangerAttributes(String ambariPassword, SharedServiceConfigsView sharedServiceConfigsView) {
        sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
        sharedServiceConfigsView.setAttachedCluster(false);
        sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
    }

    private String getDatalakeClusterManagerFqdn(Stack datalakeStack) {
        String fqdn;
        try {
            fqdn = transactionService.required(() -> datalakeStack.getNotTerminatedGatewayInstanceMetadata().isEmpty()
                    ? datalakeStack.getClusterManagerIp() : datalakeStack.getNotTerminatedGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN());
        } catch (Exception ignored) {
            LOGGER.debug("Get instance metadata transaction failed, giving back IP as FQDN");
            fqdn = datalakeStack.getClusterManagerIp();
        }
        return fqdn;
    }
}
