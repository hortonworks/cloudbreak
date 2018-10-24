package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.newway.finder.Finders.same;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.finder.Attribute;
import com.sequenceiq.it.cloudbreak.newway.finder.Finder;

public abstract class AbstractCloudbreakEntity<R, S, T extends CloudbreakEntity> extends Entity implements CloudbreakEntity, Purgable<S> {

    @Inject
    private TestParameter testParameter;

    @Inject
    private MockCloudProvider cloudProvider;

    @Inject
    private RandomNameCreator creator;

    private String name;

    private R request;

    private S response;

    private Set<S> responses;

    private TestContext testContext;

    protected AbstractCloudbreakEntity(String newId) {
        super(newId);
    }

    protected AbstractCloudbreakEntity(R request, TestContext testContext) {
        this.request = request;
        this.testContext = testContext;
    }

    public Set<S> getResponses() {
        return responses;
    }

    public void setResponses(Set<S> responses) {
        this.responses = responses;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public R getRequest() {
        return request;
    }

    public void setRequest(R request) {
        this.request = request;
    }

    public void setResponse(S response) {
        this.response = response;
    }

    public S getResponse() {
        return response;
    }

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected MockCloudProvider getCloudProvider() {
        return cloudProvider;
    }

    protected TestContext getTestContext() {
        return testContext;
    }

    @Override
    public CloudbreakEntity valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    public T when(Class<T> entityClass, ActionV2<T> action) {
        return testContext.when(entityClass, action, emptyRunningParameter());
    }

    public T when(ActionV2<T> action) {
        return testContext.when((T) this, action, emptyRunningParameter());
    }

    public T when(Class<T> entityClass, ActionV2<T> action, RunningParameter runningParameter) {
        return testContext.when(entityClass, action, runningParameter);
    }

    public T when(ActionV2<T> action, RunningParameter runningParameter) {
        return testContext.when((T) this, action, runningParameter);
    }

    public T then(AssertionV2<T> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    public T then(AssertionV2<T> assertion, RunningParameter runningParameter) {
        return testContext.then((T) this, assertion, runningParameter);
    }

    public T then(List<AssertionV2<T>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    public T then(List<AssertionV2<T>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            testContext.then((T) this, assertions.get(i), runningParameters.get(i));
        }
        return testContext.then((T) this, assertions.get(assertions.size() - 1), runningParameters.get(runningParameters.size() - 1));
    }

    public <O extends CloudbreakEntity> O given(String key, Class<O> clss) {
        return testContext.given(key, clss);
    }

    public <O extends CloudbreakEntity> O given(Class<O> clss) {
        return testContext.given(clss);
    }

    public <O> T select(Attribute<T, O> attribute) {
        return select(attribute, emptyRunningParameter());
    }

    public <O> T select(Attribute<T, O> attribute, RunningParameter runningParameter) {
        return select(attribute, same(), runningParameter);
    }

    public <O> T select(Attribute<T, O> attribute, Finder<O> finder) {
        return select(attribute, finder, emptyRunningParameter());
    }

    public <O> T select(Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
        return testContext.select((T) this, attribute, finder, runningParameter);
    }

    public <O> T capture(Attribute<T, O> attribute) {
        return capture(attribute, emptyRunningParameter());
    }

    public <O> T capture(Attribute<T, O> attribute, RunningParameter runningParameter) {
        return testContext.capture((T) this, attribute, runningParameter);
    }

    public <O> T verify(Attribute<T, O> attribute) {
        return verify(attribute, emptyRunningParameter());
    }

    public <O> T verify(Attribute<T, O> attribute, RunningParameter runningParameter) {
        return testContext.verify((T) this, attribute, runningParameter);
    }

    public T await(Class<T> entityClass, Map<String, String> statuses) {
        return await(entityClass, statuses, emptyRunningParameter());
    }

    public T await(Class<T> entityClass, Map<String, String> statuses, RunningParameter runningParameter) {
        return testContext.await(entityClass, statuses, runningParameter);
    }

    public T await(Map<String, String> statuses) {
        return await(statuses, emptyRunningParameter());
    }

    public T await(Map<String, String> statuses, RunningParameter runningParameter) {
        return testContext.await((T) this, statuses, runningParameter);
    }

    public <E extends Exception> T except(Class<E> expectedException) {
        return except(expectedException, emptyRunningParameter());
    }

    public <E extends Exception> T except(Class<E> expectedException, RunningParameter runningParameter) {
        return testContext.expect((T) this, expectedException, runningParameter);
    }

    public void validate() {
        testContext.handleExecptionsDuringTest();
    }

    public RandomNameCreator getNameCreator() {
        return creator;
    }

    @Override
    public Collection<S> getAll(CloudbreakClient client) {
        LOGGER.info("{} is not a purgable entity", getClass());
        return Collections.emptyList();
    }

    @Override
    public boolean deletable(S entity) {
        return false;
    }

    @Override
    public void delete(S entity, CloudbreakClient client) {

    }
}
