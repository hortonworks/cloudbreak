package com.sequenceiq.datalake.service.sdx;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class VerticalScaleService {

    public static final int DELETE_FAILED_RETRY_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(VerticalScaleService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private EventSender eventSender;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FlowIdentifier verticalScaleDatalake(SdxCluster sdxCluster, StackVerticalScaleV4Request request, String userCrn) {
        MDCBuilder.buildMdcContext(sdxCluster);
        LOGGER.info("Environment Vertical Scale flow triggered for environment {}", sdxCluster.getName());
        DatalakeVerticalScaleEvent environmentVerticalScaleEvent = DatalakeVerticalScaleEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceCrn(sdxCluster.getResourceCrn())
                .withResourceId(sdxCluster.getId())
                .withResourceName(sdxCluster.getName())
                .withVerticalScaleRequest(request)
                .withStackCrn(sdxCluster.getStackCrn())
                .withSelector(DatalakeVerticalScaleStateSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT.selector())
                .build();
        FlowIdentifier flowIdentifier = eventSender.sendEvent(environmentVerticalScaleEvent, new Event.Headers(getFlowTriggerUsercrn(userCrn)));
        LOGGER.debug("Environment Vertical Scale flow trigger event sent for environment {}", sdxCluster.getName());
        return flowIdentifier;
    }

    private Map<String, Object> getFlowTriggerUsercrn(String userCrn) {
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }

    public void startVerticalScale(Long id, StackVerticalScaleV4Request request) {
        SdxCluster sdxCluster = sdxService.getById(id);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();

        try {
            LOGGER.debug("Vertical scale starts in group of {} with instanceType: {}", request.getGroup(), request.getTemplate().getInstanceType());
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    stackV4Endpoint.verticalScalingByName(0L, sdxCluster.getClusterName(), initiatorUserCrn, request));
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                    "Data Lake vertical scale in progress", sdxCluster);
        } catch (NotFoundException e) {
            LOGGER.info("Cannot find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Cannot vertical scale stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException(errorMessage);
        } catch (ProcessingException e) {
            LOGGER.info("Cannot delete stack {} from cloudbreak: {}", sdxCluster.getStackId(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public StackV4Response waitCloudbreakClusterVerticalScale(Long id, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(id);
        LOGGER.debug("Waiting for vertical scale flow");
        cloudbreakPoller.pollCreateUntilVerticalScaleDone(sdxCluster, pollingConfig);
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
    }

}
