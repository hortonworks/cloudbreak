package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DataLakeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.paas.service.PaasSdxDescribeService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Service
public class DatalakeService {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    // only needed for legacy sharedresponse to skip CDL intentionally, please use PlatformAwareSdxConnector for DL functionality by default
    @Inject
    private PaasSdxDescribeService paasSdxDescribeService;

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
            fillSharedResponseOnlyForPaasDL(cluster.getEnvironmentCrn(), sharedServiceResponse);
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    public void addSharedServiceResponse(StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (!Strings.isNullOrEmpty(stackResponse.getEnvironmentCrn())) {
            LOGGER.debug("Checking datalake through the environmentCrn.");
            fillSharedResponseOnlyForPaasDL(stackResponse.getEnvironmentCrn(), sharedServiceResponse);
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

    private void fillSharedResponseOnlyForPaasDL(String environmentCrn, SharedServiceV4Response sharedServiceResponse) {
        // skipping CDL intentionally to enhance listing operations like DH list, please use PlatformAwareSdxConnector for DL functionality by default
        Optional<SdxBasicView> datalakeStack = paasSdxDescribeService.getSdxByEnvironmentCrn(environmentCrn);
        datalakeStack.ifPresent(datalake -> {
            sharedServiceResponse.setSharedClusterName(datalake.name());
            sharedServiceResponse.setSdxCrn(datalake.crn());
            sharedServiceResponse.setSdxName(datalake.name());
        });
    }

    public SharedServiceConfigsView createSharedServiceConfigsView(String password, StackType stackType, String environmentCrn) {
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

                if (StringUtils.isNotBlank(environmentCrn)) {
                    platformAwareSdxConnector.getSdxAccessViewByEnvironmentCrn(environmentCrn).ifPresent(sdxAccessView -> {
                        sharedServiceConfigsView.setDatalakeClusterManagerFqdn(sdxAccessView.clusterManagerFqdn());
                        sharedServiceConfigsView.setDatalakeClusterManagerIp(sdxAccessView.clusterManagerIp());
                        sharedServiceConfigsView.setRangerFqdn(sdxAccessView.rangerFqdn());
                    });
                }
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

    public void decorateWithDataLakeResponseAnyPlatform(StackType stackType, StackV4Response stackResponse) {
        if (!StackType.WORKLOAD.equals(stackType)) {
            LOGGER.debug("Skipping datalake decoration for non-workload stack.");
            return;
        }
        platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stackResponse.getEnvironmentCrn())
                .ifPresent(datalake -> {
                    DataLakeV4Response datalakeV4Response = new DataLakeV4Response(datalake.name(), datalake.crn(), datalake.platform().name());
                    stackResponse.setDataLakeResponse(datalakeV4Response);
                });
    }
}
