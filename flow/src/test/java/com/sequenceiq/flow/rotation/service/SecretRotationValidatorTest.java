package com.sequenceiq.flow.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType.CLUSTER_MGMT_CM_ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;

@ExtendWith(MockitoExtension.class)
class SecretRotationValidatorTest {

    @InjectMocks
    private SecretRotationValidator underTest;

    @Test
    void mapSecretTypesShouldFailWhenDuplicatedSecretTypes() {
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.mapSecretTypes(List.of("CLUSTER_CB_CM_ADMIN_PASSWORD", "CLUSTER_CB_CM_ADMIN_PASSWORD"), CloudbreakSecretType.class));
        assertEquals("There is at least one duplication in the request!", cloudbreakServiceException.getMessage());
    }

    @Test
    void mapSecretTypesShouldFailWhenInvalidEnumValue() {
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.mapSecretTypes(List.of("CLUSTER_CB_CM_ADMIN_PASSWORD", "INVALID"), CloudbreakSecretType.class));
        assertEquals("Invalid secret type, cannot map secrets [CLUSTER_CB_CM_ADMIN_PASSWORD, INVALID] to CloudbreakSecretType",
                cloudbreakServiceException.getMessage());
    }

    @Test
    void mapSecretTypesShouldSucceed() {
        List<SecretType> secretTypes =
                underTest.mapSecretTypes(List.of("CLUSTER_CB_CM_ADMIN_PASSWORD", "CLUSTER_MGMT_CM_ADMIN_PASSWORD"), CloudbreakSecretType.class);
        assertThat(secretTypes).containsExactly(CLUSTER_CB_CM_ADMIN_PASSWORD, CLUSTER_MGMT_CM_ADMIN_PASSWORD);
    }
}