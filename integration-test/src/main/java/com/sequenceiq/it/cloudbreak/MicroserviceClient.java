package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public abstract class MicroserviceClient extends Entity {

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

    public abstract <T extends WaitObject> WaitService<T> waiterService();

    public abstract <E extends Enum<E>, W extends WaitObject> W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext);
}
