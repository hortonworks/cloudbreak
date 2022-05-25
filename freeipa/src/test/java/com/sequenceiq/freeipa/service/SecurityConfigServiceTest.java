package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.DisabledSaltSecurityConfigRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;

@ExtendWith(MockitoExtension.class)
class SecurityConfigServiceTest {

    private static final String PASSWORD = "password";

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private DisabledSaltSecurityConfigRepository disabledSaltSecurityConfigRepository;

    @InjectMocks
    private SecurityConfigService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setResourceCrn("crn");
    }

    @Test
    void changeSaltPasswordOnStackWithoutSaltSecurityConfig() {
        assertThatThrownBy(() -> underTest.changeSaltPassword(stack, PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stack crn does not yet have a salt security config");
    }

    @Test
    void changePasswordSuccess() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword("old-" + PASSWORD);
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        stack.setSecurityConfig(securityConfig);

        underTest.changeSaltPassword(stack, PASSWORD);

        assertThat(saltSecurityConfig.getSaltPassword()).isEqualTo(PASSWORD);
        verify(disabledSaltSecurityConfigRepository).save(saltSecurityConfig);
    }
}
