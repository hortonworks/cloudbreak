package com.sequenceiq.datalake.service.sdx.poller;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;

@Component
public class PollerRunnerImpl implements PollerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerRunnerImpl.class);

    public PollerRunnerResult run(PollingConfig pollingConfig,
            Consumer<PollingConfig> pollingCommand,
            String processName,
            SdxCluster sdxCluster) {
        try {
            LOGGER.debug("Starting polling for {} for SDX: {}", processName, sdxCluster.getName());
            pollingCommand.accept(pollingConfig);
            LOGGER.debug("{} succeeded for SDX: {}", processName, sdxCluster.getName());
            return PollerRunnerResult.ofSuccess();
        } catch (UserBreakException userBreakException) {
            String message = String.format("%s poller exited before timeout. Cause: %s", processName, userBreakException);
            LOGGER.error(message);
            return PollerRunnerResult.ofError(userBreakException, message);
        } catch (PollerStoppedException pollerStoppedException) {
            PollerStoppedException wrapperException = new PollerStoppedException(processName + " timed out after " + pollingConfig.getDuration() + " minutes");
            String message = String.format("%s poller timed out after %d minutes.", processName, pollingConfig.getDuration());
            LOGGER.error(message);
            return PollerRunnerResult.ofError(wrapperException, message);
        } catch (PollerException exception) {
            String message = String.format("%s poller failed. Cause: %s", processName, exception);
            LOGGER.error(message);
            return PollerRunnerResult.ofError(exception, message);
        } catch (Exception e) {
            String message = String.format("%s failed. Cause: %s", processName, e);
            LOGGER.error(message);
            return PollerRunnerResult.ofError(e, message);
        }
    }

}
