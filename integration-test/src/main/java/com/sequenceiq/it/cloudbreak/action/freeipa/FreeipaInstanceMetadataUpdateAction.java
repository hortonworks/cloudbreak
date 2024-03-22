package com.sequenceiq.it.cloudbreak.action.freeipa;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static java.lang.String.format;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.constant.ImdsConstants;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imdupdate.InstanceMetadataUpdateRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeipaInstanceMetadataUpdateAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaInstanceMetadataUpdateAction.class);

    @Override
    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA instance metadata update request for environment %n"), testDto.getEnvironmentCrn());
        String currentImdsVersion = testDto.getResponse().getSupportedImdsVersion();
        InstanceMetadataUpdateType targetUpdateType = StringUtils.equals(currentImdsVersion, ImdsConstants.AWS_IMDS_VERSION_V2) ?
                IMDS_HTTP_TOKEN_OPTIONAL : IMDS_HTTP_TOKEN_REQUIRED;
        String imageImdsVersion = testDto.getResponse().getImage().getImdsVersion();
        if (IMDS_HTTP_TOKEN_REQUIRED.equals(targetUpdateType) && !StringUtils.equals(imageImdsVersion, ImdsConstants.AWS_IMDS_VERSION_V2)) {
            Log.whenJson(LOGGER, format(" FreeIPA image is not compatible regarding environment: %n"), testDto.getEnvironmentCrn());
            return testDto;
        }
        InstanceMetadataUpdateRequest request = new InstanceMetadataUpdateRequest();
        request.setEnvironmentCrn(testDto.getEnvironmentCrn());
        request.setUpdateType(targetUpdateType);
        FlowIdentifier flowIdentifier = client.getDefaultClient().getFreeIpaV1Endpoint().instanceMetadataUpdate(request);
        testDto.setFlow("FreeIPA instance metadata update",  flowIdentifier);
        Log.whenJson(LOGGER, format(" FreeIPA instance metadata update: %n"), testDto.getEnvironmentCrn());
        return testDto;
    }
}
