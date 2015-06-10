package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.polling.DummyPollingInfo;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

import reactor.fn.Pausable;
import reactor.fn.timer.Timer;

@Service
public class DummyTesterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyTesterService.class);
    private static final long DEFAULT_DELAY = 2;

    @Inject
    private Timer timer;

    @Inject
    private PollingNotifier pollingNotifier;

    public void doTest() {

        LOGGER.debug("submitted at: {}", System.nanoTime());
        PollingInfo pollingInfo = new DummyPollingInfo();

        Pausable pausable = timer.submit(PollingHandlerFactory.createStartPollingHandler(pollingInfo, pollingNotifier)
                , DEFAULT_DELAY, TimeUnit.SECONDS);
    }

}
