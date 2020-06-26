package com.sequenceiq.datalake.flow.datalake.upgrade.handler;

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
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeSuccessEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class DatalakeChangeImageWaitHandler extends ExceptionCatcherEventHandler<DatalakeChangeImageWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeChangeImageWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 60;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "DatalakeChangeImageWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        DatalakeChangeImageWaitRequest request = event.getData();
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
                response = new DatalakeUpgradeSuccessEvent(sdxId, userId);
            } else {
                String message = "Image not changed in cloudbreak side, expected image: " + expectedImageId + ", actual image: " + imageId;
                LOGGER.info(message);
                response = new DatalakeUpgradeFailedEvent(sdxId, userId, new IllegalStateException(message));
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Change image polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Change image poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake repair timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Change image polling failed for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }
}
