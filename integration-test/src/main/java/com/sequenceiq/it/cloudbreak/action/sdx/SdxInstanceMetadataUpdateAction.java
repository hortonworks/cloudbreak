package com.sequenceiq.it.cloudbreak.action.sdx;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.constant.ImdsConstants;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxInstanceMetadataUpdateRequest;

public class SdxInstanceMetadataUpdateAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInstanceMetadataUpdateAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, format(" SDX instance metadata update request for %n"), testDto.getCrn());
        String currentImdsVersion = testDto.getResponse().getStackV4Response().getSupportedImdsVersion();
        InstanceMetadataUpdateType targetUpdateType = StringUtils.equals(currentImdsVersion, ImdsConstants.AWS_IMDS_VERSION_V2) ?
                IMDS_HTTP_TOKEN_OPTIONAL : IMDS_HTTP_TOKEN_REQUIRED;
        AtomicReference<String> imageImdsVersion = new AtomicReference<>();
        testDto.getResponse().getStackV4Response().getHardwareInfoGroups().stream().findFirst().flatMap(hardwareInfoGroupV4Response ->
                hardwareInfoGroupV4Response.getHardwareInfos().stream().findFirst()).ifPresent(hardwareInfoV4Response -> {
            if (hardwareInfoV4Response.getPackageVersions().containsKey(ImagePackageVersion.IMDS_VERSION.getKey())) {
                imageImdsVersion.set(hardwareInfoV4Response.getPackageVersions().get(ImagePackageVersion.IMDS_VERSION.getKey()));
            }
        });
        if (IMDS_HTTP_TOKEN_REQUIRED.equals(targetUpdateType) && !StringUtils.equals(imageImdsVersion.get(), ImdsConstants.AWS_IMDS_VERSION_V2)) {
            Log.whenJson(LOGGER, format(" SDX image is not compatible regarding stack: %n"), testDto.getCrn());
            return testDto;
        }
        SdxInstanceMetadataUpdateRequest request = new SdxInstanceMetadataUpdateRequest();
        request.setCrn(testDto.getCrn());
        request.setUpdateType(targetUpdateType);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().instanceMetadataUpdate(request);
        testDto.setFlow("SDX instance metadata update",  flowIdentifier);
        Log.whenJson(LOGGER, format(" SDX instance metadata update: %n"), testDto.getCrn());
        return testDto;
    }
}
