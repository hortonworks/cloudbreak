package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ServiceUpgradeValidationRequestTestUtils;
import com.sequenceiq.cloudbreak.util.RsaPublicKeyTestUtil;
import com.sequenceiq.common.model.OsType;

class SshPublicKeyUpgradeValidatorTest {

    private final SshPublicKeyUpgradeValidator underTest = new SshPublicKeyUpgradeValidator();

    @Test
    void testRsa2047BitKeyFailsUpgrade() {
        ServiceUpgradeValidationRequest request = request(RsaPublicKeyTestUtil.openSshRsaKey(2047));

        UpgradeValidationFailedException ex = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(request));

        assertThat(ex).hasMessageContaining("RSA public key is 2047 bits, at least 2048 bits are required")
                .hasMessageContaining("cdp environments update-ssh-key");
    }

    @ParameterizedTest
    @ValueSource(ints = {2048, 3072, 4096})
    void testLongEnoughRsaKeyPasses(int bits) {
        ServiceUpgradeValidationRequest request = request(RsaPublicKeyTestUtil.openSshRsaKey(bits));

        assertDoesNotThrow(() -> underTest.validate(request));
    }

    @Test
    void testRsa2047BitKeyDoesNotBlockUpgradeToRhel8() {
        ServiceUpgradeValidationRequest request = request(RsaPublicKeyTestUtil.openSshRsaKey(2047), OsType.RHEL8);

        assertDoesNotThrow(() -> underTest.validate(request));
    }

    @Test
    void testEd25519KeyPasses() {
        ServiceUpgradeValidationRequest request = request("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMQltFutaGpkyuDLScqHRZtknBd4c/IJCkVsY7WFS+gK");

        assertDoesNotThrow(() -> underTest.validate(request));
    }

    @Test
    void testUnparsableKeyDoesNotBlockUpgrade() {
        ServiceUpgradeValidationRequest request = request("foo");

        assertDoesNotThrow(() -> underTest.validate(request));
    }

    @Test
    void testMissingKeyDoesNotBlockUpgrade() {
        ServiceUpgradeValidationRequest request = request(null);

        assertDoesNotThrow(() -> underTest.validate(request));
    }

    private ServiceUpgradeValidationRequest request(String publicKey) {
        return request(publicKey, OsType.RHEL9);
    }

    private ServiceUpgradeValidationRequest request(String publicKey, OsType targetOsType) {
        StackDto stack = mock(StackDto.class);
        StackAuthentication authentication = new StackAuthentication();
        authentication.setPublicKey(publicKey);
        when(stack.getStackAuthentication()).thenReturn(authentication);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.2.18", "1", targetOsType, "x86_64", null, Set.of(), null);
        return ServiceUpgradeValidationRequestTestUtils.of(stack, properties);
    }
}
