package com.sequenceiq.it.cloudbreak.context;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.performance.Measure;
import com.sequenceiq.it.cloudbreak.performance.MeasureAll;
import com.sequenceiq.it.cloudbreak.performance.PerformanceIndicator;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;
import com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak.CloudbreakWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MeasuredTestContext extends MockedTestContext {
    private TestContext wrappedTestContext;

    private Measure measure;

    private MeasuredTestContext(TestContext wrappedTestContext, Measure measure) {
        this.wrappedTestContext = wrappedTestContext;
        wrappedTestContext.setTestContext(this);
        this.measure = measure;
    }

    @Override
    public <T extends CloudbreakTestDto> T then(T entity, Class<? extends MicroserviceClient> clientClass, Assertion<T, ?
            extends MicroserviceClient> assertion, RunningParameter runningParameter) {
        return wrappedTestContext.then(entity, clientClass, assertion, runningParameter);
    }

    @Override
    public TestContext as(Actor actor) {
        wrappedTestContext.as(actor);
        return this;
    }

    @Override
    public Duration getPollingDurationInMills() {
        return wrappedTestContext.getPollingDurationInMills();
    }

    @Override
    public Map<String, CloudbreakTestDto> getResources() {
        return wrappedTestContext.getResources();
    }

    @Override
    public Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> getClients() {
        return wrappedTestContext.getClients();
    }

    @Override
    public Map<String, Exception> getExceptionMap() {
        return wrappedTestContext.getExceptionMap();
    }

    @Override
    public void setShutdown(boolean shutdown) {
        wrappedTestContext.setShutdown(shutdown);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return wrappedTestContext.getApplicationContext();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        wrappedTestContext.setApplicationContext(applicationContext);
    }

    @Override
    public WaitService<EnvironmentWaitObject> getEnvironmentWaitService() {
        return wrappedTestContext.getEnvironmentWaitService();
    }

    @Override
    public WaitService<FreeIpaWaitObject> getFreeIpaWaitService() {
        return wrappedTestContext.getFreeIpaWaitService();
    }

    @Override
    public WaitService<DatalakeWaitObject> getDatalakeWaitService() {
        return wrappedTestContext.getDatalakeWaitService();
    }

    @Override
    public WaitService<RedbeamsWaitObject> getRedbeamsWaitService() {
        return wrappedTestContext.getRedbeamsWaitService();
    }

    @Override
    public WaitService<CloudbreakWaitObject> getCloudbreakWaitService() {
        return wrappedTestContext.getCloudbreakWaitService();
    }

    @Override
    public WaitService<InstanceWaitObject> getInstanceWaitService() {
        return wrappedTestContext.getInstanceWaitService();
    }

    @Override
    protected void checkShutdown() {
        wrappedTestContext.checkShutdown();
    }

    @Override
    protected String getActingUserAccessKey() {
        return wrappedTestContext.getActingUserAccessKey();
    }

    @Override
    protected void setActingUser(CloudbreakUser actingUser) {
        wrappedTestContext.setActingUser(actingUser);
    }

    @Override
    protected CloudbreakUser getActingUser() {
        return wrappedTestContext.getActingUser();
    }

    @Override
    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        return wrappedTestContext.init(clss);
    }

    @Override
    public <O extends CloudbreakTestDto> O init(Class<O> clss, CloudPlatform cloudPlatform) {
        return wrappedTestContext.init(clss, cloudPlatform);
    }

    @Override
    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        return wrappedTestContext.given(key, clss);
    }

    @Override
    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss, CloudPlatform cloudPlatform) {
        return wrappedTestContext.given(key, clss, cloudPlatform);
    }

    @Override
    public Map<String, String> getStatuses() {
        return wrappedTestContext.getStatuses();
    }

    @Override
    public void setStatuses(Map<String, String> statusMap) {
        wrappedTestContext.setStatuses(statusMap);
    }

    @Override
    public Map<String, Exception> getErrors() {
        return wrappedTestContext.getErrors();
    }

    @Override
    public <T extends CloudbreakTestDto> T get(String key) {
        return wrappedTestContext.get(key);
    }

    @Override
    public <O> O getSelected(String key) {
        return wrappedTestContext.getSelected(key);
    }

    @Override
    public <O> O getRequiredSelected(String key) {
        return wrappedTestContext.getRequiredSelected(key);
    }

    @Override
    public <O, T extends CloudbreakTestDto> T select(T entity, Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
        return wrappedTestContext.select(entity, attribute, finder, runningParameter);
    }

    @Override
    public <O, T extends CloudbreakTestDto> T capture(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        return wrappedTestContext.capture(entity, attribute, runningParameter);
    }

    @Override
    public <O, T extends CloudbreakTestDto> T verify(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        return wrappedTestContext.verify(entity, attribute, runningParameter);
    }

    @Override
    public CloudbreakClient getCloudbreakClient(String who) {
        return wrappedTestContext.getCloudbreakClient(who);
    }

    @Override
    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends MicroserviceClient> msClientClass, String who) {
        return wrappedTestContext.getMicroserviceClient(msClientClass, who);
    }

    @Override
    public CloudbreakClient getCloudbreakClient() {
        return wrappedTestContext.getCloudbreakClient();
    }

    @Override
    public SdxClient getSdxClient(String who) {
        return wrappedTestContext.getSdxClient(who);
    }

    @Override
    public SdxClient getSdxClient() {
        return wrappedTestContext.getSdxClient();
    }

    @Override
    public <U extends MicroserviceClient> U getMicroserviceClient(Class<U> msClientClass) {
        return wrappedTestContext.getMicroserviceClient(msClientClass);
    }

    @Override
    public <T extends SdxTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        return wrappedTestContext.awaitForFlow(entity, runningParameter);
    }

    @Override
    public <T extends SdxInternalTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        return wrappedTestContext.awaitForFlow(entity, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        return wrappedTestContext.awaitForFlow(entity, runningParameter);
    }

    @Override
    public <T extends FreeIpaTestDto, E extends com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto, E extends Status> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter, pollingInterval);
    }

    @Override
    public <T extends EnvironmentTestDto, E extends EnvironmentStatus> T await(T entity, E desiredStatus, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter);
    }

    @Override
    public <T extends EnvironmentTestDto, E extends EnvironmentStatus> T await(T entity, E desiredStatus, RunningParameter runningParameter,
            Duration pollingInterval) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter, pollingInterval);
    }

    @Override
    public <T extends SdxTestDto, E extends SdxClusterStatusResponse> T await(T entity, E desiredStatus, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter);
    }

    @Override
    public <T extends SdxInternalTestDto, E extends SdxClusterStatusResponse> T await(T entity, E desiredStatus, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter);
    }

    @Override
    public <T extends RedbeamsDatabaseServerTestDto, E extends com.sequenceiq.redbeams.api.model.common.Status> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatus, runningParameter);
    }

    @Override
    public <T extends DistroXTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter);
    }

    @Override
    public <T extends DistroXTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter, pollingInterval);
    }

    @Override
    public <T extends SdxTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter);
    }

    @Override
    public <T extends SdxTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter, pollingInterval);
    }

    @Override
    public <T extends SdxInternalTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter);
    }

    @Override
    public <T extends SdxInternalTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return wrappedTestContext.await(entity, desiredStatuses, runningParameter, pollingInterval);
    }

    @Override
    public <E extends Exception, T extends CloudbreakTestDto> T expect(T entity, Class<E> expectedException, RunningParameter runningParameter) {
        return wrappedTestContext.expect(entity, expectedException, runningParameter);
    }

    @Override
    public void handleExceptionsDuringTest(TestErrorLog silently) {
        wrappedTestContext.handleExceptionsDuringTest(silently);
    }

    @Override
    public void cleanupTestContext() {
        wrappedTestContext.cleanupTestContext();
    }

    @Override
    public void shutdown() {
        wrappedTestContext.shutdown();
    }

    @Override
    public CloudProviderProxy getCloudProvider() {
        return wrappedTestContext.getCloudProvider();
    }

    @Override
    protected <T extends CloudbreakTestDto, U extends MicroserviceClient>
    T doAction(T entity, Class<? extends MicroserviceClient> clientClass, Action<T, U> action, String who) throws Exception {
        PerformanceIndicator pi = new PerformanceIndicator(action.getClass().getSimpleName());
        T result = wrappedTestContext.doAction(entity, clientClass, action, who);
        measure.add(pi);
        return result;
    }

    public static MeasuredTestContext createMeasuredTestContext(TestContext testContext) {
        return new MeasuredTestContext(testContext, new MeasureAll());
    }

    public TestContext getWrappedTestContext() {
        return wrappedTestContext;
    }

    public Measure getMeasure() {
        return measure;
    }

    protected TestContext getTestContext() {
        return this;
    }

    @Override
    public SparkServer getSparkServer() {
        if (wrappedTestContext instanceof MockedTestContext) {
            return ((MockedTestContext) wrappedTestContext).getSparkServer();
        } else {
            throw new IllegalArgumentException("Not a Mock TestContext");
        }
    }

    @Override
    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        if (wrappedTestContext instanceof MockedTestContext) {
            return ((MockedTestContext) wrappedTestContext).getImageCatalogMockServerSetup();
        } else {
            throw new IllegalArgumentException("Not a Mock TestContext");
        }
    }

    @Override
    public DefaultModel getModel() {
        if (wrappedTestContext instanceof MockedTestContext) {
            return ((MockedTestContext) wrappedTestContext).getModel();
        } else {
            throw new IllegalArgumentException("Not a Mock TestContext");
        }
    }

    @Override
    public DynamicRouteStack dynamicRouteStack() {
        if (wrappedTestContext instanceof MockedTestContext) {
            return ((MockedTestContext) wrappedTestContext).dynamicRouteStack();
        } else {
            throw new IllegalArgumentException("Not a Mock TestContext");
        }
    }

    @Override
    protected <T extends CloudbreakTestDto> T getEntityFromEntityClass(Class<T> entityClass, RunningParameter runningParameter) {
        return wrappedTestContext.getEntityFromEntityClass(entityClass, runningParameter);
    }

    @Override
    public CloudbreakUser getWho(RunningParameter runningParameter) {
        return wrappedTestContext.getWho(runningParameter);
    }

    @Override
    public String toString() {
        return super.toString() + wrappedTestContext.toString();
    }
}
