package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplicationContext.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
public abstract class AbstractComponentTest<T> {

    @Inject
    private EventBus eb;

    @Inject
    private ParameterGenerator g;

    protected T sendCloudRequest() {
        CloudPlatformRequest<T> request = getRequest();
        return sendCloudRequest(request);
    }

    protected T sendCloudRequest(CloudPlatformRequest<T> request) {
        eb.notify(getTopicName(), Event.wrap(request));
        try {
            return request.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected abstract String getTopicName();

    protected abstract CloudPlatformRequest<T> getRequest();

    protected ParameterGenerator g() {
        return g;
    }

}
