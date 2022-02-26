package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDatalakeCertificateTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.cloudbreak.CloudbreakInstanceWaitObject;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxServiceApiKeyClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyEndpoints;

public class SdxClient extends MicroserviceClient<SdxServiceApiKeyEndpoints, Void, SdxClusterStatusResponse, DatalakeWaitObject> {
    public static final String SDX_CLIENT = "SDX_CLIENT";

    private SdxServiceApiKeyEndpoints sdxClient;

    SdxClient() {
        super(SDX_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return sdxClient.flowPublicEndpoint();
    }

    @Override
    public DatalakeWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, SdxClusterStatusResponse> desiredStatuses,
            TestContext testContext, Set<SdxClusterStatusResponse> ignoredFailedStatuses) {
        return new DatalakeWaitObject(this, entity.getName(), desiredStatuses.get("status"), ignoredFailedStatuses);
    }

    @Override
    public SdxServiceApiKeyEndpoints getDefaultClient() {
        return sdxClient;
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient(String key) {
        return testContext -> testContext.getContextParam(key, SdxClient.class);
    }

    public static synchronized SdxClient createProxySdxClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        SdxClient clientEntity = new SdxClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.sdxClient = new SdxServiceApiKeyClient(
                testParameter.get(SdxTest.SDX_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(SdxTestDto.class.getSimpleName(),
                RenewDatalakeCertificateTestDto.class.getSimpleName(),
                SdxInternalTestDto.class.getSimpleName(),
                SdxCustomTestDto.class.getSimpleName(),
                SdxChangeImageCatalogTestDto.class.getSimpleName(),
                SdxEventTestDto.class.getSimpleName());
    }

    @Override
    public <O extends Enum<O>> InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus) {
        return new CloudbreakInstanceWaitObject(testContext, entity.getName(), instanceIds, (InstanceStatus) instanceStatus);
    }
}

