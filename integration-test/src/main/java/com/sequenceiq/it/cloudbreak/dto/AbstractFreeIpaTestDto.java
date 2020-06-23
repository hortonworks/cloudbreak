package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;

public abstract class AbstractFreeIpaTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, FreeIpaClient> {

    @Inject
    private WaitUtil waitUtil;

    protected AbstractFreeIpaTestDto(String newId) {
        super(newId);
    }

    protected AbstractFreeIpaTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public T when(Class<T> entityClass, Action<T, FreeIpaClient> action) {
        return getTestContext().when(entityClass, FreeIpaClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Action<T, FreeIpaClient> action) {
        return getTestContext().when((T) this, FreeIpaClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Class<T> entityClass, Action<T, FreeIpaClient> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, FreeIpaClient.class, action, runningParameter);
    }

    @Override
    public T when(Action<T, FreeIpaClient> action, RunningParameter runningParameter) {
        return getTestContext().when((T) this, FreeIpaClient.class, action, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, FreeIpaClient> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), FreeIpaClient.class, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public T then(Assertion<T, FreeIpaClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public T then(Assertion<T, FreeIpaClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((T) this, FreeIpaClient.class, assertion, runningParameter);
    }

    @Override
    public T then(List<Assertion<T, FreeIpaClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public T then(List<Assertion<T, FreeIpaClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((T) this, FreeIpaClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext().then((T) this, FreeIpaClient.class, assertions.get(assertions.size() - 1), runningParameters.get(runningParameters.size() - 1));
    }

    public WaitUtil getWaitUtil() {
        return waitUtil;
    }
}
