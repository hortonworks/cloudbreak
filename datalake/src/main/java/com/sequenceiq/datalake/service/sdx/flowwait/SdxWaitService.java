package com.sequenceiq.datalake.service.sdx.flowwait;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerException;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class SdxWaitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxWaitService.class);

    @Inject
    private SdxService sdxService;

    /**
     * Will start a wait task on an SDX cluster.
     * For the frequency of checks and the total amount of time allowed for the task, please check {@linkplain PollingConfig} in the waitTask
     *
     * This wait service uses {@linkplain com.dyngr.Polling} as the underlying polling mechanism, meaning:
     * - if the polling succeeds, the method returns
     * - if there is a timeout, or the wait task wants to indicate a negative result, then an exception is thrown of type {@linkplain PollerException}
     * - optionally you can set to stop on any encountered exceptions, check {@linkplain PollingConfig.stopPollingIfExceptionOccured} for that
     *
     * @param sdxWaitTask A wait task with polling parameters as well as the algorithm for polling itself
     * @param <V> The type of parameter returned by the underlying poller package's AttemptMaker interface, hidden from outside world
     * @throws SdxWaitException is thrown if a {@linkplain PollerException} is reached
     */
    public <V> void waitFor(SdxWaitTask<V> sdxWaitTask) {
        try {
            LOGGER.debug("Starting an sdx wait task: {}", sdxWaitTask);
            PollingConfig pollingConfig = sdxWaitTask.getPollingConfig();
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(sdxWaitTask);
        } catch (PollerException e) {
            LOGGER.warn("Polling of {} reached an exception: ", sdxWaitTask.getPollingMessage(), e);
            throw new SdxWaitException(String.format("Polling of %s reached an error state.", sdxWaitTask.getPollingMessage()), e);
        }
    }

}
