package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxScaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDatalakeCertificateTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.cloudbreak.CloudbreakInstanceWaitObject;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxInternalCrnClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyEndpoints;
import com.sequenceiq.sdx.client.SdxServiceCrnEndpoints;
import com.sequenceiq.sdx.client.SdxServiceUserCrnClient;
import com.sequenceiq.sdx.client.SdxServiceUserCrnClientBuilder;

public class SdxClient extends MicroserviceClient<SdxServiceApiKeyEndpoints, SdxServiceCrnEndpoints, SdxClusterStatusResponse, DatalakeWaitObject> {

    private SdxServiceApiKeyEndpoints sdxClient;

    private SdxInternalCrnClient sdxInternalClient;

    private SdxServiceApiKeyEndpoints alternativeSdxClient;

    private SdxInternalCrnClient alternativeSdxInternalClient;

    public SdxClient(CloudbreakUser cloudbreakUser, String sdxAddress, String sdxInternalAddress,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator, String alternativeSdxAddress, String alternativeSdxInternalAddress) {
        setActing(cloudbreakUser);
        sdxClient = createSdxClient(cloudbreakUser, sdxAddress);
        sdxInternalClient = createInternalSdxClient(sdxInternalAddress, regionAwareInternalCrnGenerator);

        if (isNotEmpty(alternativeSdxAddress) && isNotEmpty(alternativeSdxInternalAddress)) {
            alternativeSdxClient = createSdxClient(cloudbreakUser, alternativeSdxAddress);
            alternativeSdxInternalClient = createInternalSdxClient(alternativeSdxInternalAddress, regionAwareInternalCrnGenerator);
        }
    }

    private SdxServiceApiKeyEndpoints createSdxClient(CloudbreakUser cloudbreakUser, String sdxAddress) {
        return new SdxServiceApiKeyClient(
                sdxAddress,
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeSdxClient.flowPublicEndpoint();
        } else {
            return sdxClient.flowPublicEndpoint();
        }
    }

    @Override
    public DatalakeWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, SdxClusterStatusResponse> desiredStatuses,
            TestContext testContext, Set<SdxClusterStatusResponse> ignoredFailedStatuses) {
        return new DatalakeWaitObject(this, entity.getName(), desiredStatuses.get("status"), ignoredFailedStatuses, testContext);
    }

    @Override
    public SdxServiceApiKeyEndpoints getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeSdxClient;
        } else {
            return sdxClient;
        }
    }

    @Override
    public SdxServiceCrnEndpoints getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        SdxInternalCrnClient client;
        if (testContext.shouldUseAlternativeEndpoints()) {
            client = alternativeSdxInternalClient;
        } else {
            client = sdxInternalClient;
        }
        return client.withInternalCrn();
    }

    private static synchronized SdxInternalCrnClient createInternalSdxClient(String serverRoot,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        SdxServiceUserCrnClient userCrnClient = new SdxServiceUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new SdxInternalCrnClient(userCrnClient, regionAwareInternalCrnGenerator);
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(SdxTestDto.class.getSimpleName(),
                RenewDatalakeCertificateTestDto.class.getSimpleName(),
                SdxInternalTestDto.class.getSimpleName(),
                SdxChangeImageCatalogTestDto.class.getSimpleName(),
                SdxEventTestDto.class.getSimpleName(),
                SdxScaleTestDto.class.getSimpleName());
    }

    @Override
    public <O extends Enum<O>> InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus) {
        return new CloudbreakInstanceWaitObject(testContext, entity.getName(), instanceIds, (InstanceStatus) instanceStatus);
    }
}

