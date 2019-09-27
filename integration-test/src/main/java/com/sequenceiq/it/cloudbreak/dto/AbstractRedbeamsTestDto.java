package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class AbstractRedbeamsTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, RedbeamsClient> {

    protected AbstractRedbeamsTestDto(String newId) {
        super(newId);
    }

    protected AbstractRedbeamsTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public T when(Class<T> entityClass, Action<T, RedbeamsClient> action) {
        return getTestContext().when(entityClass, RedbeamsClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Action<T, RedbeamsClient> action) {
        return getTestContext().when((T) this, RedbeamsClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Class<T> entityClass, Action<T, RedbeamsClient> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, RedbeamsClient.class, action, runningParameter);
    }

    @Override
    public T when(Action<T, RedbeamsClient> action, RunningParameter runningParameter) {
        return getTestContext().when((T) this, RedbeamsClient.class, action, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, RedbeamsClient> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), RedbeamsClient.class, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public T then(Assertion<T, RedbeamsClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public T then(Assertion<T, RedbeamsClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((T) this, RedbeamsClient.class, assertion, runningParameter);
    }

    @Override
    public T then(List<Assertion<T, RedbeamsClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public T then(List<Assertion<T, RedbeamsClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((T) this, RedbeamsClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext()
                .then((T) this, RedbeamsClient.class, assertions.get(assertions.size() - 1), runningParameters.get(runningParameters.size() - 1));
    }
}
