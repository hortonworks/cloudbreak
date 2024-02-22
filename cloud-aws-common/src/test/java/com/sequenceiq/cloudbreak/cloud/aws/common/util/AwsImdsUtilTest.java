package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.cloud.UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.services.ec2.model.HttpTokensState;

@ExtendWith(MockitoExtension.class)
class AwsImdsUtilTest {

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsImdsUtil underTest;

    @Test
    void testGetHttpTokensState() {
        assertEquals(HttpTokensState.REQUIRED, AwsImdsUtil.getHttpTokensStateByUpdateType(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED));
        assertEquals(HttpTokensState.OPTIONAL, AwsImdsUtil.getHttpTokensStateByUpdateType(INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL));
        assertThrows(CloudbreakServiceException.class, () -> AwsImdsUtil.getHttpTokensStateByUpdateType(UpdateType.IMAGE_UPDATE));
    }

    @Test
    void testValidateIfNotEntitled() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.FALSE);
        assertThrows(CloudbreakServiceException.class, () ->
                underTest.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of()), authenticatedContext("acc")));
    }

    @Test
    void testValidateIfEmptyPackageVersions() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertThrows(CloudbreakServiceException.class, () ->
                underTest.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of()), authenticatedContext("acc")));
    }

    @Test
    void testValidateIfPackageVersionMissing() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertThrows(CloudbreakServiceException.class, () ->
                underTest.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of()), authenticatedContext("acc")));
    }

    @Test
    void testValidateIfNotCompatibleImage() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertThrows(CloudbreakServiceException.class, () -> underTest.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED,
                cloudStack(Map.of("imds", "v1")), authenticatedContext("acc")));
    }

    @Test
    void testValidate() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        underTest.validateInstanceMetadataUpdate(INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, cloudStack(Map.of("imds", "v2")), authenticatedContext("acc"));
    }

    private CloudStack cloudStack(Map<String, String> packageVersions) {
        Image image = new Image(null, Map.of(), null, null, null, null, null, packageVersions, null, null);
        return new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null, null, null, null);
    }

    private AuthenticatedContext authenticatedContext(String accountId) {
        return new AuthenticatedContext(CloudContext.Builder.builder().withAccountId(accountId).build(), null);
    }
}
