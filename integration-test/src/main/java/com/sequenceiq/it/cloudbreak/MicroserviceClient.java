package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;

public abstract class MicroserviceClient<C, I, E extends Enum<E>, W extends WaitObject> extends Entity {

    protected static final int TIMEOUT = 60 * 1000;

    private CloudbreakUser acting;

    protected MicroserviceClient(String newId) {
        super(newId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + acting + "]";
    }

    public void setActing(CloudbreakUser acting) {
        this.acting = acting;
    }

    public CloudbreakUser getActing() {
        return acting;
    }

    public Set<String> supportedTestDtos() {
        return Set.of();
    }

    public abstract FlowPublicEndpoint flowPublicEndpoint();

    public <T extends WaitObject> WaitService<T> waiterService() {
        return new WaitService<>();
    }

    public W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext, Set<E> ignoredFailedStatuses) {
        throw new TestFailException("Wait object is not supported by the client");
    }

    public <O extends Enum<O>>  InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus, Set<O> ignoredFailedStatuses) {
        throw new TestFailException("Can't create waitInstanceWaitObject instances object");
    }

    public abstract C getDefaultClient();

    public I getInternalClient(TestContext testContext) {
        throw new TestFailException("There is no internal client for this microservice");
    }

    public void checkIfInternalClientAllowed(TestContext testContext) {
        if (!(testContext instanceof MockedTestContext)) {
            throw new TestFailException("You can use internal client only for mock tests!");
        }
    }
}
