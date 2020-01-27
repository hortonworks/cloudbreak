package com.sequenceiq.it.cloudbreak.dto.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.RequestData;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.spark.SparkServerPool;

@Prototype
public class HttpMock implements CloudbreakTestDto {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpMock.class);

    @Inject
    private TestParameter testParameter;

    @Inject
    @Qualifier("cloudProviderProxy")
    private CloudProvider cloudProvider;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private SparkServerPool sparkServerPool;

    private String name;

    private TestContext testContext;

    private List<RequestData> requestList = new LinkedList<>();

    private DefaultModel model;

    private DynamicRouteStack dynamicRouteStack;

    private SparkServer sparkServer;

    protected HttpMock(TestContext testContext) {
        this.testContext = testContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return null;
    }

    @Override
    public String getLastKnownFlowId() {
        return null;
    }

    @Override
    public CloudbreakTestDto valid() {
        if (testContext instanceof MockedTestContext) {
            MockedTestContext testContext = (MockedTestContext) this.testContext;
            model = testContext.getModel();
            dynamicRouteStack = testContext.dynamicRouteStack();
            sparkServer = testContext.getSparkServer();
        } else {
            LOGGER.info("Creating HttpMock server");
            sparkServer = sparkServerPool.popSecure();
            LOGGER.info("HttpMock got spark server: {}", sparkServer);
            model = new DefaultModel();
            model.setMockServerAddress(mockServerAddress);
            dynamicRouteStack = new DynamicRouteStack(sparkServer.getSparkService(), model);
        }
        return this;
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

    public ResourcePropertyProvider getResourcePropertyProvider() {
        return resourcePropertyProvider;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name: " + getName() + "]";
    }

    public <T> T whenRequested(Class<T> endpoint) {
        return (T) Proxy.newProxyInstance(
                HttpMock.class.getClassLoader(),
                new Class[]{endpoint},
                (proxy, method, args) -> {
                    Method httpMethod = Method.build(method.getName());
                    SparkUriParameters parameters = new SparkUriAnnotationHandler(endpoint, method).getParameters();
                    return method.getReturnType().getConstructor(Method.class, String.class, Class.class, HttpMock.class)
                            .newInstance(httpMethod, parameters.getUri(), parameters.getType(), this);

                });
    }

    public <T> String getUrl(Class<T> endpoint, java.lang.reflect.Method method) {
        SparkUriParameters parameters = new SparkUriAnnotationHandler(endpoint, method).getParameters();
        return sparkServer.getEndpoint() + parameters.getUri();
    }

    public List<RequestData> getRequestList() {
        return requestList;
    }

    public <T> T then(Class<T> endpoint) {
        return whenRequested(endpoint);
    }

    public HttpMock then(Assertion<HttpMock, CloudbreakClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    public HttpMock then(Assertion<HttpMock, CloudbreakClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((HttpMock) this, CloudbreakClient.class, assertion, runningParameter);
    }

    public HttpMock then(List<Assertion<HttpMock, CloudbreakClient>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    public HttpMock then(List<Assertion<HttpMock, CloudbreakClient>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then(this, CloudbreakClient.class, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext().then(this, CloudbreakClient.class, assertions.get(assertions.size() - 1),
                runningParameters.get(runningParameters.size() - 1));
    }

    public void validate() {
        testContext.handleExceptionsDuringTest(false);
    }

    public DefaultModel getModel() {
        return model;
    }

    public DynamicRouteStack getDynamicRouteStack() {
        return dynamicRouteStack;
    }

    public SparkServer getSparkServer() {
        return sparkServer;
    }
}
