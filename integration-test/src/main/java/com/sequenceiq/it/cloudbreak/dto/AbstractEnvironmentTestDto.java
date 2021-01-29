package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;

public abstract class AbstractEnvironmentTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, EnvironmentClient> {

    @Inject
    private FlowUtil flowUtil;

    protected AbstractEnvironmentTestDto(String newId) {
        super(newId);
    }

    protected AbstractEnvironmentTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public T when(Class<T> entityClass, Action<T, EnvironmentClient> action) {
        return getTestContext().when(entityClass, EnvironmentClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Action<T, EnvironmentClient> action) {
        return getTestContext().when((T) this, EnvironmentClient.class, action, emptyRunningParameter());
    }

    public T assignRole(Action<T, UmsClient> action) {
        return getTestContext().when((T) this, UmsClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Class<T> entityClass, Action<T, EnvironmentClient> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, EnvironmentClient.class, action, runningParameter);
    }

    @Override
    public T when(Action<T, EnvironmentClient> action, RunningParameter runningParameter) {
        return getTestContext().when((T) this, EnvironmentClient.class, action, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, EnvironmentClient> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), EnvironmentClient.class, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, EnvironmentClient> action, Class<E> expectedException) {
        return getTestContext().whenException(entityClass, EnvironmentClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Action<T, EnvironmentClient> action, Class<E> expectedException) {
        return getTestContext().whenException((T) this, EnvironmentClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, EnvironmentClient> action, Class<E> expectedException,
            RunningParameter runningParameter) {
        return getTestContext().whenException(entityClass, EnvironmentClient.class, action, expectedException, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Action<T, EnvironmentClient> action, Class<E> expectedException, RunningParameter runningParameter) {
        return getTestContext().whenException((T) this, EnvironmentClient.class, action, expectedException, runningParameter);
    }

    @Override
    public T then(Assertion<T, EnvironmentClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public T then(Assertion<T, EnvironmentClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((T) this, EnvironmentClient.class, assertion, runningParameter);
    }

    @Override
    public T then(List<Assertion<T, EnvironmentClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public T then(List<Assertion<T, EnvironmentClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((T) this, EnvironmentClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext()
                .then((T) this, EnvironmentClient.class, assertions.get(assertions.size() - 1), runningParameters.get(runningParameters.size() - 1));
    }

    public FlowUtil getFlowUtil() {
        return flowUtil;
    }
}