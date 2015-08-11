package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

import reactor.bus.Event;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplicationContext.class)
public class LaunchStackComponentTest {


    @Inject
    private CloudConnector cloudConnector;

    @Inject
    private ResourceConnector resourceConnector;

    @Inject
    private ParameterGenerator g;

    @Inject
    private LaunchStackHandler launchStackHandler;


    @Inject
    private PersistenceNotifier notifier;


    @Test
    public void testLaunchStack() {
        CloudContext ctx = g.createCloudContext();
        CloudCredential cred = g.createCloudCredential();
        AuthenticatedContext ac = new AuthenticatedContext(ctx, cred);
        CloudStack cs = g.createCloudStack();
        LaunchStackRequest lr = new LaunchStackRequest(ctx, cred, cs);

        when(cloudConnector.authenticate(ctx, cred)).thenReturn(ac);

        //when(resourceConnector.launch(ac, cs, notifier )).thenReturn(ac);

        launchStackHandler.accept(Event.wrap(lr));
    }


}
