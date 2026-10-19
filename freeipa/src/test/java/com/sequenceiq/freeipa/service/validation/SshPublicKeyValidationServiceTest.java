package com.sequenceiq.freeipa.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.RsaPublicKeyTestUtil;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;

class SshPublicKeyValidationServiceTest {

    private final SshPublicKeyValidationService underTest = new SshPublicKeyValidationService();

    @Test
    void testRejectsShortRsaKeyForRhel9() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateForTargetOs(stackWithKey(RsaPublicKeyTestUtil.openSshRsaKey(2047)), OsType.RHEL9));

        assertThat(exception).hasMessageContaining("RSA public key is 2047 bits, at least 2048 bits are required")
                .hasMessageContaining("cdp environments update-ssh-key");
    }

    @Test
    void testCreationRejectsShortRsaKeyForRhel9WithoutUpdateSshKeyAdvice() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateForCreation(stackWithKey(RsaPublicKeyTestUtil.openSshRsaKey(2047)), OsType.RHEL9));

        assertThat(exception).hasMessageContaining("RSA public key is 2047 bits, at least 2048 bits are required")
                .hasMessageContaining("RSA key of at least 2048 bits or an Ed25519 key")
                .hasMessageNotContaining("update-ssh-key");
    }

    @Test
    void testCreationAllowsShortRsaKeyForRhel8() {
        assertDoesNotThrow(() -> underTest.validateForCreation(stackWithKey(RsaPublicKeyTestUtil.openSshRsaKey(2047)), OsType.RHEL8));
    }

    @Test
    void testAllowsShortRsaKeyForRhel8() {
        assertDoesNotThrow(() -> underTest.validateForTargetOs(stackWithKey(RsaPublicKeyTestUtil.openSshRsaKey(2047)), OsType.RHEL8));
    }

    @Test
    void testAllowsLongEnoughRsaKeyForRhel9() {
        assertDoesNotThrow(() -> underTest.validateForTargetOs(stackWithKey(RsaPublicKeyTestUtil.openSshRsaKey(3072)), OsType.RHEL9));
    }

    @Test
    void testAllowsMissingKeyForRhel9() {
        assertDoesNotThrow(() -> underTest.validateForTargetOs(stackWithKey(null), OsType.RHEL9));
        assertDoesNotThrow(() -> underTest.validateForTargetOs(new Stack(), OsType.RHEL9));
    }

    private Stack stackWithKey(String publicKey) {
        Stack stack = new Stack();
        StackAuthentication authentication = new StackAuthentication();
        authentication.setPublicKey(publicKey);
        stack.setStackAuthentication(authentication);
        return stack;
    }
}
