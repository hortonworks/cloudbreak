package com.sequenceiq.datalake.flow.datalake.upgrade.handler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeVmReplaceEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class DatalakeChangeImageWaitHandler extends ExceptionCatcherEventHandler<DatalakeChangeImageWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeChangeImageWaitHandler.class);

    @Value("${sdx.stack.change.image.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.change.image.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeChangeImageWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeChangeImageWaitRequest> event) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        DatalakeChangeImageWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling change image process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Change image");
            String imageId = upgradeService.getImageId(sdxId);
            String expectedImageId = request.getUpgradeOption().getUpgrade().getImageId();
            if (Objects.equals(imageId, expectedImageId)) {
                LOGGER.info("Image changed in cloudbreak side for SDX {}, actual image: {}", sdxId, imageId);
                response = new DatalakeVmReplaceEvent(sdxId, userId);
            } else {
                String message = String.format("Image not changed in cloudbreak side, expected image: %s, actual image: %s", expectedImageId, imageId);
                LOGGER.info(message);
                response = new DatalakeUpgradeFailedEvent(sdxId, userId, new IllegalStateException(message));
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Change image polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Change image poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("Change image poller timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Change image polling failed for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
