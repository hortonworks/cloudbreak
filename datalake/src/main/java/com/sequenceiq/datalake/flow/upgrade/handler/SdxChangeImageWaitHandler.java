package com.sequenceiq.datalake.flow.upgrade.handler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.flow.upgrade.event.SdxChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.upgrade.event.SdxImageChangedEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SdxChangeImageWaitHandler implements EventHandler<SdxChangeImageWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxChangeImageWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 60;

    @Inject
    private EventBus eventBus;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "SdxChangeImageWaitRequest";
    }

    @Override
    public void accept(Event<SdxChangeImageWaitRequest> event) {
        SdxChangeImageWaitRequest request = event.getData();
        MDCBuilder.addRequestId(request.getRequestId());
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        String requestId = request.getRequestId();
        Selectable response;
        try {
            LOGGER.info("Start polling change image process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Change image");
            String imageId = upgradeService.getImageId(sdxId);
            String expectedImageId = request.getUpgradeOption().getUpgrade().getImageId();
            if (Objects.equals(imageId, expectedImageId)) {
                response = new SdxImageChangedEvent(sdxId, userId, requestId);
            } else {
                String message = "Image not changed in cloudbreak side, expected image: " + expectedImageId + ", actial image: " + imageId;
                LOGGER.info(message);
                response = new SdxUpgradeFailedEvent(sdxId, userId, requestId, new IllegalStateException(message));
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Change image polling exited before timeout. Cause: ", userBreakException);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Change image poller stopped for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Datalake repair timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Change image polling failed for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
