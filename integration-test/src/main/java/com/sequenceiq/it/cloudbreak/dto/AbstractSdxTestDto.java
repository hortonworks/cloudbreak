package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.search.ClusterLogsStorageUrl;
import com.sequenceiq.it.cloudbreak.search.StorageUrl;
import com.sequenceiq.it.cloudbreak.util.yarn.YarnCloudFunctionality;

public abstract class AbstractSdxTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, SdxClient> {

    @Inject
    private YarnCloudFunctionality yarnCloudFunctionality;

    private CloudPlatform cloudPlatformFromStack;

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

    public T awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public T awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance((T) this, statuses, runningParameter);
    }

    public boolean hasSpotTermination(StackV4Response stackResponse) {
        return stackResponse != null && stackResponse.getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
    }

    public void setCloudPlatformFromStack(StackV4Response stackResponse) {
        if (stackResponse != null) {
            cloudPlatformFromStack = stackResponse.getCloudPlatform();
        }
    }

    public String getSdxBaseLocation() {
        TelemetryResponse telemetryResponse = (getTestContext().get(EnvironmentTestDto.class) != null)
                ? getTestContext().get(EnvironmentTestDto.class).getResponse().getTelemetry()
                : null;
        return (telemetryResponse != null)
                ? telemetryResponse.getLogging().getStorageLocation()
                : null;
    }

    @Override
    public String getCloudStorageUrl(String resourceName, String resourceCrn) {
        if (CloudPlatform.YARN.equalsIgnoreCase(getCloudPlatform().name())) {
            LOGGER.info("Special case for AWS-YCloud Hybrid tests. " +
                    "Here the defined Cloud Provider is AWS and the Cluster Logs are stored at AWS. " +
                    "However the Datalake has been created at YCloud. So the Base Location for logs are also different.");
            return yarnCloudFunctionality.getDataLakeS3LogsUrl(resourceCrn);
        } else {
            CloudProviderProxy cloudProviderProxy = getTestContext().getCloudProvider();
            StorageUrl storageUrl = new ClusterLogsStorageUrl();
            return (isCloudProvider(cloudProviderProxy) && StringUtils.isNotBlank(getSdxBaseLocation()))
                    ? storageUrl.getDatalakeStorageUrl(resourceName, resourceCrn, getSdxBaseLocation(), cloudProviderProxy)
                    : null;
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return (cloudPlatformFromStack != null) ? cloudPlatformFromStack : super.getCloudPlatform();
    }
}
