package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxAutotlsCertRotationAction implements Action<SdxTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAutotlsCertRotationAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        CertificatesRotationV4Request certificatesRotationV4Request = new CertificatesRotationV4Request();
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX autotls cert rotation request: ", certificatesRotationV4Request);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint()
                .rotateAutoTlsCertificatesByName(testDto.getName(), new CertificatesRotationV4Request());
        testDto.setFlow("SDX autotls cert rotation", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext).sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX response after autotls cert rotation: ", detailedResponse);
        return testDto;
    }
}
