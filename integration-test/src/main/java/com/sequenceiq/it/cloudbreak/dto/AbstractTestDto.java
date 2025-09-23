package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.finder.Finders.same;
import static java.lang.String.format;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.context.TestErrorLog;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ClouderaManagerEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.DatabaseEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ExperienceEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.FreeIPAEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.SaltEndpoints;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.SpiEndpoints;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.search.SearchUrl;
import com.sequenceiq.it.cloudbreak.search.SearchUrlFactory;
import com.sequenceiq.it.cloudbreak.search.Searchable;

public abstract class AbstractTestDto<R, S, T extends CloudbreakTestDto, U extends MicroserviceClient> implements CloudbreakTestDto {

    @Inject
    @Qualifier("cloudProviderProxy")
    private CloudProvider cloudProvider;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private ExecuteQueryToMockInfrastructure executeQuery;

    private String name;

    private R request;

    private S response;

    private Set<S> responses;

    private TestContext testContext;

    private String lastKnownFlowChainId;

    private String lastKnownFlowId;

    private CloudPlatform cloudPlatform;

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

    public String getResourceNameType() {
        return null;
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

    protected CloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public TestContext getTestContext() {
        return testContext.getTestContext();
    }

    @Override
    public String getLastKnownFlowChainId() {
        return lastKnownFlowChainId;
    }

    public void setLastKnownFlowChainId(String lastKnownFlowChainId) {
        this.lastKnownFlowChainId = lastKnownFlowChainId;
        this.lastKnownFlowId = null;
    }

    @Override
    public String getLastKnownFlowId() {
        return lastKnownFlowId;
    }

    @Override
    public void setLastKnownFlowId(String lastKnownFlowId) {
        this.lastKnownFlowId = lastKnownFlowId;
        this.lastKnownFlowChainId = null;
    }

    @Override
    public void setPrivateIpsForLogCollection(TestContext testContext) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    @Override
    public Map<HostGroupType, String> getPrivateIpsForLogCollection() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
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

    public <E extends Exception> T thenException(Assertion<T, U> assertion, Class<E> expectedException, RunningParameter runningParameter) {
        throw new NotImplementedException(
                String.format("The entity(%s) must be implement the thenException(Assertion<T, U>, Class<E>, RunningParameter) method.", getClass()));
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        return getTestContext().given(key, clss);
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss, CloudPlatform cloudPlatform) {
        return getTestContext().given(key, clss, cloudPlatform);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss) {
        return getTestContext().given(clss);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss, CloudPlatform cloudPlatform) {
        return getTestContext().given(clss, cloudPlatform);
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        return getTestContext().init(clss);
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
        return getTestContext().select((T) this, attribute, finder, runningParameter);
    }

    public <O> T capture(Attribute<T, O> attribute) {
        return capture(attribute, emptyRunningParameter());
    }

    public <O> T capture(Attribute<T, O> attribute, RunningParameter runningParameter) {
        return getTestContext().capture((T) this, attribute, runningParameter);
    }

    public <O> T verify(Attribute<T, O> attribute) {
        return verify(attribute, emptyRunningParameter());
    }

    public <O> T verify(Attribute<T, O> attribute, RunningParameter runningParameter) {
        return getTestContext().verify((T) this, attribute, runningParameter);
    }

    public T await(Class<T> entityClass, Map<String, Status> statuses) {
        return getTestContext().await(entityClass, statuses, emptyRunningParameter());
    }

    public T await(Class<T> entityClass, Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await(entityClass, statuses, runningParameter);
    }

    public T await(Map<String, Status> statuses) {
        return await(statuses, emptyRunningParameter());
    }

    public T awaitWithClient(Map<String, Status> statuses, U client) {
        return getTestContext().awaitWithClient((T) this, statuses, client);
    }

    public T await(Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await((T) this, statuses, runningParameter);
    }

    public T awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow((T) this, runningParameter);
    }

    public T awaitForFlow() {
        return getTestContext().awaitForFlow((T) this, emptyRunningParameter());
    }

    public <E extends Exception> T expect(Class<E> expectedException) {
        return expect(expectedException, emptyRunningParameter());
    }

    public <E extends Exception> T expect(Class<E> expectedException, RunningParameter runningParameter) {
        return getTestContext().expect((T) this, expectedException, runningParameter);
    }

    public void validate() {
        getTestContext().handleExceptionsDuringTest(TestErrorLog.FAIL);
    }

    public void skipWhenFailure() {
        getTestContext().handleExceptionsDuringTest(TestErrorLog.SKIP);
    }

    public ResourcePropertyProvider getResourcePropertyProvider() {
        return resourcePropertyProvider;
    }

    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, U> action, RunningParameter runningParameter) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }

    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, U> action, Class<E> expectedException) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the whenException(Class<T>, Action<T, U>, Class<E>) method.",
                getClass()));
    }

    public <E extends Exception> T whenException(Action<T, U> action, Class<E> expectedException) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the when(Action<T, U>, Class<E>) method.", getClass()));
    }

    public <E extends Exception> T whenException(Class<T> entityClass, Action<T, U> action, Class<E> expectedException,
            RunningParameter runningParameter) {
        throw new NotImplementedException(
                String.format("The entity(%s) must be implement the when(Class<T>, Action<T, U>, Class<E>, RunningParameter) method.", getClass()));
    }

    public <E extends Exception> T whenException(Action<T, U> action, Class<E> expectedException, RunningParameter runningParameter) {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the when(Action<T, U>, Class<E>, RunningParameter) method.",
                getClass()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name: " + getName() + ']';
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    @Override
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public void setFlow(String marker, FlowIdentifier flowIdentifier) {
        if (flowIdentifier != null) {
            switch (flowIdentifier.getType()) {
                case FLOW:
                    Log.when(LOGGER, format(" %s flow %s started ", marker, flowIdentifier.getPollableId()));
                    setLastKnownFlowId(flowIdentifier.getPollableId());
                    break;
                case FLOW_CHAIN:
                    Log.when(LOGGER, format(" %s flow chain %s started ", marker, flowIdentifier.getPollableId()));
                    setLastKnownFlowChainId(flowIdentifier.getPollableId());
                    break;
                case NOT_TRIGGERED:
                    Log.when(LOGGER, format(" %s flow was not triggered ", marker));
                    break;
                default:
                    throw new TestFailException("Flow identifier's type must be FLOW or FLOW_CHAIN. Current value: " + flowIdentifier);
            }
        } else {
            throw new TestFailException("Flow identifier is not present. " +
                    "Make sure you use an endpoint which triggers a flow/flow chain and gives back it's identifier.");
        }
    }

    public T disableVerification() {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/calls/" + crn + "/disable", Collections.emptyMap(), null, r -> r, w -> w);
        return (T) this;
    }

    public T enableVerification() {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/calls/enable", Collections.emptyMap(), null, r -> r, w -> w.queryParam("mockUuid", crn));
        return (T) this;
    }

    public T setNewInstanceFailure(int failedInstanceCount) {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/" + crn + "/spi/new_instance_failure", Map.of(), null, r -> r,
                w -> w.queryParam("failedInstanceCount", failedInstanceCount));
        return (T) this;
    }

    public T setSaltRunFailure(String command) {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/{mock_uuid}/failure/salt_run", Map.of(), null, r -> r,
                w -> w.resolveTemplate("mock_uuid", crn).queryParam("run_arg", command));
        return (T) this;
    }

    public T scheduleFailureOnCommand(String command, String group, int allNodeCount, int failedNodeCount) {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/{mock_uuid}/failure", Map.of(), null, r -> r,
                w -> w.resolveTemplate("mock_uuid", crn)
                        .queryParam("run_arg", command)
                        .queryParam("group", group)
                        .queryParam("all_node_count", allNodeCount)
                        .queryParam("failed_node_count", failedNodeCount));
        return (T) this;
    }

    public T deleteSaltRunFailure(String command) {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.DELETE, "/{mock_uuid}/failure/salt_run", Map.of(), null, r -> r,
                w -> w.resolveTemplate("mock_uuid", crn).queryParam("run_arg", command));
        return (T) this;
    }

    public T enableVerification(String path) {
        executeQuery.executeMethod(HttpMethod.POST, "/calls/enable", Collections.emptyMap(), null, r -> r, w -> w.queryParam("mockUuid", path));
        return (T) this;
    }

    public T resetCalls() {
        String crn = getCrn();
        executeQuery.executeMethod(HttpMethod.POST, "/calls/reset", Collections.emptyMap(), null, r -> r, w -> w.queryParam("mockUuid", crn));
        return (T) this;
    }

    public SpiEndpoints<T> mockSpi() {
        if (getTestContext() instanceof MockedTestContext) {
            return new SpiEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockSpi is supported by MockedTestContext only.");
    }

    public SaltEndpoints<T> mockSalt() {
        if (getTestContext() instanceof MockedTestContext) {
            return new SaltEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockSalt is supported by MockedTestContext only.");
    }

    public ExperienceEndpoints<T> mockExperience() {
        if (getTestContext() instanceof MockedTestContext) {
            return new ExperienceEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockLiftie is supported by MockedTestContext only.");
    }

    public ClouderaManagerEndpoints<T> mockCm() {
        if (getTestContext() instanceof MockedTestContext) {
            return new ClouderaManagerEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockCm is supported by MockedTestContext only.");
    }

    public FreeIPAEndpoints<T> mockFreeIpa() {
        if (getTestContext() instanceof MockedTestContext) {
            return new FreeIPAEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockFreeIpa is supported by MockedTestContext only.");
    }

    public DatabaseEndpoints<T> mockCloudDatabase() {
        if (getTestContext() instanceof MockedTestContext) {
            return new DatabaseEndpoints<>((T) this, (MockedTestContext) getTestContext());
        }
        throw new TestFailException("mockCloudDatabase is supported by MockedTestContext only");
    }

    public U getClientForCleanup() {
        String crn = getCrn();
        if (crn == null) {
            return null;
        }
        String accountId = Crn.safeFromString(crn).getAccountId();
        return getTestContext().getAdminMicroserviceClient(getClass(), accountId);
    }

    public boolean isCloudProvider(CloudProviderProxy cloudProvider) {
        return !CloudPlatform.MOCK.equalsIgnoreCase(cloudProvider.getCloudPlatform().name());
    }

    public String getCloudStorageUrl(String resourceName, String resourceCrn) {
        throw new NotImplementedException(
                String.format("The entity(%s) must be implement the getCloudStorageUrl(String resourceName, String resourceCrn) method.", getClass()));
    }

    public String getLogSearchUrl(List<Searchable> searchables) {
        Date testStartDate = getTestContext().getTestStartTime();
        Date testStopDate = getTestContext().getTestEndTime();
        CloudProviderProxy cloudProviderProxy = getTestContext().getCloudProvider();
        if (CloudPlatform.MOCK.equalsIgnoreCase(cloudProviderProxy.getCloudPlatform().name())) {
            return null;
        } else {
            SearchUrl searchUrl = SearchUrlFactory.getSearchUrl();
            return searchUrl.getSearchUrl(searchables, testStartDate, testStopDate);
        }
    }

    public boolean govCloud() {
        return getCloudProvider().getGovCloud();
    }
}
