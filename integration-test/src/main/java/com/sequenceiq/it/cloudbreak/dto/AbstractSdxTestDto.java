package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;

public abstract class AbstractSdxTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, SdxClient> {

    @Inject
    private FlowUtil flowUtil;

    protected AbstractSdxTestDto(String newId) {
        super(newId);
    }

    protected AbstractSdxTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public T when(Class<T> entityClass, Action<T, SdxClient> action) {
        return getTestContext().when(entityClass, SdxClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Action<T, SdxClient> action) {
        return getTestContext().when((T) this, SdxClient.class, action, emptyRunningParameter());
    }

    @Override
    public T when(Class<T> entityClass, Action<T, SdxClient> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, SdxClient.class, action, runningParameter);
    }

    @Override
    public T when(Action<T, SdxClient> action, RunningParameter runningParameter) {
        return getTestContext().when((T) this, SdxClient.class, action, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, SdxClient> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), SdxClient.class, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, SdxClient> action, Class<E> expectedException) {
        return getTestContext().whenException(entityClass, SdxClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Action<T, SdxClient> action, Class<E> expectedException) {
        return getTestContext().whenException((T) this, SdxClient.class, action, expectedException, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, SdxClient> action, Class<E> expectedException,
            RunningParameter runningParameter) {
        return getTestContext().whenException(entityClass, SdxClient.class, action, expectedException, runningParameter);
    }

    @Override
    public <E extends Exception> T whenException(Action<T, SdxClient> action, Class<E> expectedException, RunningParameter runningParameter) {
        return getTestContext().whenException((T) this, SdxClient.class, action, expectedException, runningParameter);
    }

    @Override
    public T then(Assertion<T, SdxClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public T then(Assertion<T, SdxClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((T) this, SdxClient.class, assertion, runningParameter);
    }

    @Override
    public T then(List<Assertion<T, SdxClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public T then(List<Assertion<T, SdxClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((T) this, SdxClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext().then((T) this, SdxClient.class, assertions.get(assertions.size() - 1),
                runningParameters.get(runningParameters.size() - 1));
    }

    @Override
    public void deleteForCleanup(SdxClient client) {
        try {
            setFlow("SDX deletion", client.getDefaultClient().sdxEndpoint().deleteByCrn(getCrn(), true));
            awaitForFlow();
        } catch (NotFoundException nfe) {
            LOGGER.info("resource not found, thus cleanup not needed.");
        }
    }
}