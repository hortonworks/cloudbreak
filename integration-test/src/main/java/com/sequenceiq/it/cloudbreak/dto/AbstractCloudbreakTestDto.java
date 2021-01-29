package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class AbstractCloudbreakTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, CloudbreakClient> {

    protected AbstractCloudbreakTestDto(String newId) {
        super(newId);
    }

    protected AbstractCloudbreakTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public T when(Class<T> entityClass, Action<T, CloudbreakClient> action) {
        return getTestContext().when(entityClass, CloudbreakClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Action<T, CloudbreakClient> action) {
        return getTestContext().when((T) this, CloudbreakClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Class<T> entityClass, Action<T, CloudbreakClient> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, CloudbreakClient.class, action, runningParameter);
    }

    @Override
    public T when(Action<T, CloudbreakClient> action, RunningParameter runningParameter) {
        return getTestContext().when((T) this, CloudbreakClient.class, action, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, CloudbreakClient> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), CloudbreakClient.class, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, CloudbreakClient> action, Class<E> expectedException) {
        return getTestContext().whenException(entityClass, CloudbreakClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Action<T, CloudbreakClient> action, Class<E> expectedException) {
        return getTestContext().whenException((T) this, CloudbreakClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, CloudbreakClient> action, Class<E> expectedException,
            RunningParameter runningParameter) {
        return getTestContext().whenException(entityClass, CloudbreakClient.class, action, expectedException, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Action<T, CloudbreakClient> action, Class<E> expectedException, RunningParameter runningParameter) {
        return getTestContext().whenException((T) this, CloudbreakClient.class, action, expectedException, runningParameter);
    }

    @Override
    public T then(Assertion<T, CloudbreakClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public T then(Assertion<T, CloudbreakClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((T) this, CloudbreakClient.class, assertion, runningParameter);
    }

    @Override
    public T then(List<Assertion<T, CloudbreakClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public T then(List<Assertion<T, CloudbreakClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((T) this, CloudbreakClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext().then((T) this, CloudbreakClient.class, assertions.get(assertions.size() - 1),
                runningParameters.get(runningParameters.size() - 1));
    }
}