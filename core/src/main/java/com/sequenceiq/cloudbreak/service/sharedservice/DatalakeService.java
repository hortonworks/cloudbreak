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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Service
public class DatalakeService {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public void prepareDatalakeRequest(Stack source, StackV4Request stackRequest) {
        if (!Strings.isNullOrEmpty(source.getEnvironmentCrn())) {
            LOGGER.debug("Prepare datalake request by environmentCrn");
            SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
            Optional<SdxBasicView> datalakeStack = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(source.getEnvironmentCrn());
            datalakeStack.ifPresent(s -> sharedServiceRequest.setDatalakeName(s.name()));
            stackRequest.setSharedService(sharedServiceRequest);
        }
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
}
