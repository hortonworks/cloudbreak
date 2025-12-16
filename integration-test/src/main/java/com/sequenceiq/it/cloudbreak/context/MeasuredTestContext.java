package com.sequenceiq.it.cloudbreak.context;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.config.user.TestUsers;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.microservice.TestClients;
import com.sequenceiq.it.cloudbreak.performance.Measure;
import com.sequenceiq.it.cloudbreak.performance.MeasureAll;
import com.sequenceiq.it.cloudbreak.performance.PerformanceIndicator;
import com.sequenceiq.it.cloudbreak.util.wait.service.ResourceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceAwait;

public class MeasuredTestContext extends MockedTestContext {
    private TestContext wrappedTestContext;

    private Measure measure;

    private MeasuredTestContext(TestContext wrappedTestContext, Measure measure) {
        this.wrappedTestContext = wrappedTestContext;
        wrappedTestContext.setTestContext(this);
        setTestContext(wrappedTestContext);
        this.measure = measure;
    }

    @Override
    public int getMaxRetry() {
        return wrappedTestContext.getMaxRetry();
    }

    @Override
    public int getMaxRetryCount() {
        return wrappedTestContext.getMaxRetryCount();
    }

    @Override
    public long getPollingInterval() {
        return wrappedTestContext.getPollingInterval();
    }

    @Override
    public Duration getPollingDurationOrTheDefault(RunningParameter runningParameter) {
        return wrappedTestContext.getPollingDurationOrTheDefault(runningParameter);
    }

    @Override
    public <T extends CloudbreakTestDto> T then(T entity, Class<? extends MicroserviceClient> clientClass, Assertion<T, ?
            extends MicroserviceClient> assertion, RunningParameter runningParameter) {
        return wrappedTestContext.then(entity, clientClass, assertion, runningParameter);
    }

    @Override
    public TestContext as() {
        return wrappedTestContext.as();
    }

    @Override
    public TestContext as(CloudbreakUser cloudbreakUser) {
        return wrappedTestContext.as(cloudbreakUser);
    }

    @Override
    public TestUsers getTestUsers() {
        return wrappedTestContext.getTestUsers();
    }

    @Override
    public TestClients getTestClients() {
        return wrappedTestContext.getTestClients();
    }

    @Override
    public Map<String, CloudbreakTestDto> getResourceNames() {
        return wrappedTestContext.getResourceNames();
    }

    @Override
    public Map<String, CloudbreakTestDto> getResourceCrns() {
        return wrappedTestContext.getResourceCrns();
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
    public InstanceAwait getInstanceAwait() {
        return wrappedTestContext.getInstanceAwait();
    }

    @Override
    public ResourceAwait getResourceAwait() {
        return wrappedTestContext.getResourceAwait();
    }

    @Override
    protected void checkShutdown() {
        wrappedTestContext.checkShutdown();
    }

    @Override
    public String getActingUserAccessKey() {
        return wrappedTestContext.getActingUserAccessKey();
    }

    @Override
    public Crn getActingUserCrn() {
        return wrappedTestContext.getActingUserCrn();
    }

    @Override
    public String getActingUserName() {
        return wrappedTestContext.getActingUserName();
    }

    @Override
    public void setActingUser(CloudbreakUser actingUser) {
        wrappedTestContext.setActingUser(actingUser);
    }

    @Override
    public CloudbreakUser setActingUser(RunningParameter runningParameter) {
        return wrappedTestContext.setActingUser(runningParameter);
    }

    @Override
    public CloudbreakUser getActingUser() {
        return wrappedTestContext.getActingUser();
    }

    @Override
    public String getActingUserOwnerTag() {
        return wrappedTestContext.getActingUserOwnerTag();
    }

    @Override
    public String getWorkloadUserName() {
        return wrappedTestContext.getWorkloadUserName();
    }

    @Override
    public String getCreationTimestampTag() {
        return wrappedTestContext.getCreationTimestampTag();
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
    public <U extends MicroserviceClient> U getAdminMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String accountId) {
        return wrappedTestContext.getAdminMicroserviceClient(testDtoClass, accountId);
    }

    @Override
    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String who) {
        return wrappedTestContext.getMicroserviceClient(testDtoClass, who);
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
    public CloudbreakClient getCloudbreakClient(String who) {
        return wrappedTestContext.getCloudbreakClient(who);
    }

    @Override
    public CloudbreakClient getCloudbreakClient() {
        return wrappedTestContext.getCloudbreakClient();
    }

    @Override
    public <U extends MicroserviceClient> U getMicroserviceClient(Class<U> msClientClass) {
        return wrappedTestContext.getMicroserviceClient(msClientClass);
    }

    @Override
    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        return wrappedTestContext.awaitForFlow(entity, runningParameter);
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
    public CloudPlatform getCloudPlatform() {
        return wrappedTestContext.getCloudPlatform();
    }

    @Override
    public void waitingFor(Duration duration, String interruptedMessage) {
        wrappedTestContext.waitingFor(duration, interruptedMessage);
    }

    @Override
    public Tunnel getTunnel() {
        return wrappedTestContext.getTunnel();
    }

    @Override
    public boolean isResourceEncryptionEnabled() {
        return wrappedTestContext.isResourceEncryptionEnabled();
    }

    @Override
    public void skipSafeLogicValidation() {
        wrappedTestContext.skipSafeLogicValidation();
    }

    @Override
    public boolean getSafeLogicValidation() {
        return wrappedTestContext.getSafeLogicValidation();
    }

    @Override
    public void setValidateSelinux(boolean validateSelinux) {
        wrappedTestContext.setValidateSelinux(validateSelinux);
    }

    @Override
    public boolean getSELinuxValidation() {
        return wrappedTestContext.getSELinuxValidation();
    }

    @Override
    public void checkNonEmpty(String name, String value) {
        wrappedTestContext.checkNonEmpty(name, value);
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

    public Measure getMeasure() {
        return measure;
    }

    @Override
    protected <T extends CloudbreakTestDto> T getEntityFromEntityClass(Class<T> entityClass, RunningParameter runningParameter) {
        return wrappedTestContext.getEntityFromEntityClass(entityClass, runningParameter);
    }

    @Override
    public String toString() {
        return super.toString() + wrappedTestContext.toString();
    }
}
