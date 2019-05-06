package com.sequenceiq.it.cloudbreak.newway.dto;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.newway.finder.Finders.same;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.finder.Attribute;
import com.sequenceiq.it.cloudbreak.newway.finder.Finder;

public abstract class AbstractCloudbreakTestDto<R, S, T extends CloudbreakTestDto> extends Entity implements CloudbreakTestDto {

    @Inject
    private TestParameter testParameter;

    @Inject
    @Qualifier("cloudProviderProxy")
    private CloudProvider cloudProvider;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    private String name;

    private R request;

    private S response;

    private Set<S> responses;

    private TestContext testContext;

    protected AbstractCloudbreakTestDto(String newId) {
        super(newId);
    }

    protected AbstractCloudbreakTestDto(R request, TestContext testContext) {
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

    public TestParameter getTestParameter() {
        return testParameter;
    }

    protected CloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public TestContext getTestContext() {
        return testContext;
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    public T when(Class<T> entityClass, Action<T> action) {
        return testContext.when(entityClass, action, emptyRunningParameter());
    }

    public T when(Action<T> action) {
        return testContext.when((T) this, action, emptyRunningParameter());
    }

    public T when(Class<T> entityClass, Action<T> action, RunningParameter runningParameter) {
        return testContext.when(entityClass, action, runningParameter);
    }

    public T when(Action<T> action, RunningParameter runningParameter) {
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

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        return testContext.given(key, clss);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss) {
        return testContext.given(clss);
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        return testContext.init(clss);
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

    public T await(Class<T> entityClass, Map<String, Status> statuses) {
        return await(entityClass, statuses, emptyRunningParameter());
    }

    public T await(Class<T> entityClass, Map<String, Status> statuses, long pollingInteval) {
        return testContext.await(entityClass, statuses, emptyRunningParameter(), pollingInteval);
    }

    public T await(Class<T> entityClass, Map<String, Status> statuses, RunningParameter runningParameter) {
        return testContext.await(entityClass, statuses, runningParameter);
    }

    public T await(Map<String, Status> statuses) {
        return await(statuses, emptyRunningParameter());
    }

    public T await(Map<String, Status> statuses, RunningParameter runningParameter) {
        return testContext.await((T) this, statuses, runningParameter);
    }

    public <E extends Exception> T expect(Class<E> expectedException) {
        return expect(expectedException, emptyRunningParameter());
    }

    public <E extends Exception> T expect(Class<E> expectedException, RunningParameter runningParameter) {
        return testContext.expect((T) this, expectedException, runningParameter);
    }

    public void validate() {
        testContext.handleExecptionsDuringTest(false);
    }

    public ResourcePropertyProvider resourceProperyProvider() {
        return resourcePropertyProvider;
    }

    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T> action, RunningParameter runningParameter) {
        testContext.when((T) testContext.given(clazz), action, runningParameter);
        return testContext.expect((T) testContext.given(clazz), BadRequestException.class, runningParameter);
    }
}