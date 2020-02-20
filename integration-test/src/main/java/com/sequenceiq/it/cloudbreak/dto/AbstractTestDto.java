package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.finder.Finders.same;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.Entity;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Finder;

public abstract class AbstractTestDto<R, S, T extends CloudbreakTestDto, U extends MicroserviceClient> extends Entity implements CloudbreakTestDto {

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

    private String lastKnownFlowChainId;

    private String lastKnownFlowId;

    private CloudPlatform cloudPlatform;

    protected AbstractTestDto(String newId) {
        super(newId);
    }

    protected AbstractTestDto(R request, TestContext testContext) {
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
    public String getLastKnownFlowChainId() {
        return lastKnownFlowChainId;
    }

    public void setLastKnownFlowChainId(String lastKnownFlowChainId) {
        this.lastKnownFlowChainId = lastKnownFlowChainId;
    }

    @Override
    public String getLastKnownFlowId() {
        return lastKnownFlowId;
    }

    public void setLastKnownFlowId(String lastKnownFlowId) {
        this.lastKnownFlowId = lastKnownFlowId;
    }

    @Override
    public CloudbreakTestDto valid() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    public T when(Class<T> entityClass, Action<T, U> action) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the when(Class<T>, Action<T, U>) method.", getClass()));
    }

    public T when(Action<T, U> action) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the when(Action<T, U>) method.", getClass()));
    }

    public T when(Class<T> entityClass, Action<T, U> action, RunningParameter runningParameter) {
        throw new NotImplementedException(
                String.format("The entity(%s) must be implement the when(Class<T>, Action<T, U>, RunningParameter) method.", getClass()));
    }

    public T when(Action<T, U> action, RunningParameter runningParameter) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the when(Action<T, U>, RunningParameter) method.", getClass()));
    }

    public T then(Assertion<T, U> assertion) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the then(Assertion<T, U>) method.", getClass()));
    }

    public T then(Assertion<T, U> assertion, RunningParameter runningParameter) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the then(Assertion<T, U>, RunningParameter) method.", getClass()));
    }

    public T then(List<Assertion<T, U>> assertions) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the then(List<Assertion<T, U>>) method.", getClass()));
    }

    public T then(List<Assertion<T, U>> assertions, List<RunningParameter> runningParameters) {
        throw new NotImplementedException(
                String.format("The entity(%s) must be implement the then(List<Assertion<T, U>>, List<RunningParameter>) method.", getClass()));
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        return testContext.given(key, clss);
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss, CloudPlatform cloudPlatform) {
        return testContext.given(key, clss, cloudPlatform);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss) {
        return testContext.given(clss);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss, CloudPlatform cloudPlatform) {
        return testContext.given(clss, cloudPlatform);
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

    public T awaitForFlow(RunningParameter runningParameter) {
        return testContext.awaitForFlow((T) this, runningParameter);
    }

    public <E extends Exception> T expect(Class<E> expectedException) {
        return expect(expectedException, emptyRunningParameter());
    }

    public <E extends Exception> T expect(Class<E> expectedException, RunningParameter runningParameter) {
        return testContext.expect((T) this, expectedException, runningParameter);
    }

    public void validate() {
        testContext.handleExceptionsDuringTest(false);
    }

    public ResourcePropertyProvider getResourcePropertyProvider() {
        return resourcePropertyProvider;
    }

    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, U> action, RunningParameter runningParameter) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name: " + getName() + "]";
    }

    protected void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    protected CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }
}
