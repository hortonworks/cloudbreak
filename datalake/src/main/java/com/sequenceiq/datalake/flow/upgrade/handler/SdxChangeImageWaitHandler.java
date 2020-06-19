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
import com.sequenceiq.datalake.flow.upgrade.event.SdxChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.upgrade.event.SdxImageChangedEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class SdxChangeImageWaitHandler extends ExceptionCatcherEventHandler<SdxChangeImageWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxChangeImageWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 60;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "SdxChangeImageWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        SdxChangeImageWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling change image process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Change image");
            String imageId = upgradeService.getImageId(sdxId);
            String expectedImageId = request.getUpgradeOption().getUpgrade().getImageId();
            if (Objects.equals(imageId, expectedImageId)) {
                response = new SdxImageChangedEvent(sdxId, userId);
            } else {
                String message = "Image not changed in cloudbreak side, expected image: " + expectedImageId + ", actial image: " + imageId;
                LOGGER.info(message);
                response = new SdxUpgradeFailedEvent(sdxId, userId, new IllegalStateException(message));
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Change image polling exited before timeout. Cause: ", userBreakException);
            response = new SdxUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Change image poller stopped for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake repair timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Change image polling failed for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }
}
