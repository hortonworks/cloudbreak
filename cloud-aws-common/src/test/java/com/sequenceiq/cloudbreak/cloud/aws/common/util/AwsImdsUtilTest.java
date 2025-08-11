package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.services.ec2.model.HttpTokensState;

@ExtendWith(MockitoExtension.class)
class AwsImdsUtilTest {

    @Test
    void testGetHttpTokensState() {
        assertEquals(HttpTokensState.REQUIRED, AwsImdsUtil.getHttpTokensStateByUpdateType(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED));
        assertEquals(HttpTokensState.OPTIONAL, AwsImdsUtil.getHttpTokensStateByUpdateType(INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL));
        assertThrows(CloudbreakServiceException.class, () -> AwsImdsUtil.getHttpTokensStateByUpdateType(UpdateType.IMAGE_UPDATE));
    }

    @Test
    void testValidateIfNotEntitled() {
        assertThrows(CloudbreakServiceException.class, () ->
                AwsImdsUtil.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of())));
    }

    @Test
    void testValidateIfEmptyPackageVersions() {
        assertThrows(CloudbreakServiceException.class, () ->
                AwsImdsUtil.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of())));
    }

    @Test
    void testValidateIfPackageVersionMissing() {
        assertThrows(CloudbreakServiceException.class, () ->
                AwsImdsUtil.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of())));
    }

    @Test
    void testValidateIfNotCompatibleImage() {
        assertThrows(CloudbreakServiceException.class, () ->
                AwsImdsUtil.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of("imds", "v1"))));
    }

    @Test
    void testValidate() {
        AwsImdsUtil.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of("imds", "v2")));
    }

    private CloudStack cloudStack(Map<String, String> packageVersions) {
        Image image = new Image(null, Map.of(), null, null, null, null, null, null, packageVersions, null, null, null);
        return CloudStack.builder()
                .image(image)
                .build();
    }
}
