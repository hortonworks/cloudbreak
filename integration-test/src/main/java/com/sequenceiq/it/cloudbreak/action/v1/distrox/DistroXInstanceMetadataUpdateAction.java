package com.sequenceiq.it.cloudbreak.action.v1.distrox;

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
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXInstanceMetadataUpdateV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXInstanceMetadataUpdateAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXInstanceMetadataUpdateAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" DistroX instance metadata update request for %n"), testDto.getCrn());
        String currentImdsVersion = testDto.getResponse().getSupportedImdsVersion();
        InstanceMetadataUpdateType targetUpdateType = StringUtils.equals(currentImdsVersion, ImdsConstants.AWS_IMDS_VERSION_V2) ?
                IMDS_HTTP_TOKEN_OPTIONAL : IMDS_HTTP_TOKEN_REQUIRED;
        AtomicReference<String> imageImdsVersion = new AtomicReference<>();
        testDto.getResponse().getHardwareInfoGroups().stream().findFirst().flatMap(hardwareInfoGroupV4Response ->
                hardwareInfoGroupV4Response.getHardwareInfos().stream().findFirst()).ifPresent(hardwareInfoV4Response -> {
            if (hardwareInfoV4Response.getPackageVersions().containsKey(ImagePackageVersion.IMDS_VERSION.getKey())) {
                imageImdsVersion.set(hardwareInfoV4Response.getPackageVersions().get(ImagePackageVersion.IMDS_VERSION.getKey()));
            }
        });
        if (IMDS_HTTP_TOKEN_REQUIRED.equals(targetUpdateType) && !StringUtils.equals(imageImdsVersion.get(), ImdsConstants.AWS_IMDS_VERSION_V2)) {
            Log.whenJson(LOGGER, format(" DistroX image is not compatible regarding stack: %n"), testDto.getCrn());
            return testDto;
        }
        DistroXInstanceMetadataUpdateV1Request request = new DistroXInstanceMetadataUpdateV1Request();
        request.setCrn(testDto.getCrn());
        request.setUpdateType(targetUpdateType);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).distroXV1Endpoint().instanceMetadataUpdate(request);
        testDto.setFlow("DistroX instance metadata update",  flowIdentifier);
        Log.whenJson(LOGGER, format(" DistroX instance metadata update: %n"), testDto.getCrn());
        return testDto;
    }
}
