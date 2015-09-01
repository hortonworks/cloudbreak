package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;

import reactor.bus.Event;
import reactor.bus.EventBus;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplicationContext.class)
public abstract class AbstractComponentTest<T> {
    @Inject
    private EventBus eb;

    @Inject
    private ParameterGenerator g;

    protected T sendCloudRequest() {
        CloudPlatformRequest request = getRequest();
        return sendCloudRequest(request);
    }

    protected T sendCloudRequest(CloudPlatformRequest request) {
        eb.notify(getTopicName(), Event.wrap(request));
        try {
            return (T) request.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getTopicName();

    protected abstract CloudPlatformRequest getRequest();

    protected ParameterGenerator g() {
        return g;
    }

}
