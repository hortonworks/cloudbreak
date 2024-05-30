package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class DatalakeService {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private StackService stackService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private TransactionService transactionService;

    public void prepareDatalakeRequest(Stack source, StackV4Request stackRequest) {
        if (!Strings.isNullOrEmpty(source.getEnvironmentCrn())) {
            LOGGER.debug("Prepare datalake request by environmentCrn");
            SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
            Optional<SdxBasicView> datalakeStack = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(source.getEnvironmentCrn());
            datalakeStack.ifPresent(s -> sharedServiceRequest.setDatalakeName(s.name()));
            stackRequest.setSharedService(sharedServiceRequest);
        }
    }

    public void addSharedServiceResponse(ClusterApiView cluster, ClusterViewV4Response clusterResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (cluster.getEnvironmentCrn() != null) {
            LOGGER.debug("Add shared service response by environmentCrn");
            Optional<SdxBasicView> datalakeStack = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(cluster.getEnvironmentCrn());
            datalakeStack.ifPresent(datalake -> {
                sharedServiceResponse.setSharedClusterName(datalake.name());
                sharedServiceResponse.setSdxCrn(datalake.crn());
                sharedServiceResponse.setSdxName(datalake.name());
            });
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    public void addSharedServiceResponse(StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (!Strings.isNullOrEmpty(stackResponse.getEnvironmentCrn())) {
            LOGGER.debug("Checking datalake through the environmentCrn.");
            Optional<SdxBasicView> datalakeStack = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stackResponse.getEnvironmentCrn());
            if (datalakeStack.isPresent()) {
                SdxBasicView datalake = datalakeStack.get();
                sharedServiceResponse.setSharedClusterName(datalake.name());
                sharedServiceResponse.setSdxCrn(datalake.crn());
                sharedServiceResponse.setSdxName(datalake.name());
            } else {
                LOGGER.debug("Unable to find datalake by environment CRN {}", stackResponse.getEnvironmentCrn());
            }
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

    public Optional<Stack> getDatalakeStackByDatahubStack(StackView datahubStack) {
        if (!Strings.isNullOrEmpty(datahubStack.getDatalakeCrn())) {
            return Optional.ofNullable(stackService.getByCrnOrElseNull(datahubStack.getDatalakeCrn()));
        }
        LOGGER.info("There is no datalake has been set for the cluster.");
        return Optional.empty();
    }

    public SharedServiceConfigsView createSharedServiceConfigsView(String password, StackType stackType, String datalakeCrn) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();

        switch (stackType) {
            case DATALAKE:
                setRangerAttributes(password, sharedServiceConfigsView);
                sharedServiceConfigsView.setDatalakeCluster(true);
                break;
            case WORKLOAD:
                sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
                sharedServiceConfigsView.setDatalakeCluster(false);
                sharedServiceConfigsView.setAttachedCluster(true);

                if (Strings.isNullOrEmpty(datalakeCrn)) {
                    break;
                }
                Stack datalakeStack = stackService.getByCrnOrElseNull(datalakeCrn);
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
            fqdn = transactionService.required(() -> datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().isEmpty()
                    ? datalakeStack.getClusterManagerIp()
                    : datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN());
        } catch (Exception ignored) {
            LOGGER.debug("Get instance metadata transaction failed, giving back IP as FQDN");
            fqdn = datalakeStack.getClusterManagerIp();
        }
        return fqdn;
    }
}
