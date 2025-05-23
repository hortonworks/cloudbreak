package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.SaltSecurityConfigRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SecurityConfigServiceTest {

    private static final String PASSWORD = "password";

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private SaltSecurityConfigRepository saltSecurityConfigRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

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
        saltSecurityConfig.setSaltPasswordVault("old" + PASSWORD);
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        stack.setSecurityConfig(securityConfig);

        underTest.changeSaltPassword(stack, PASSWORD);

        assertEquals(PASSWORD, saltSecurityConfig.getSaltPasswordVault());
        verify(saltSecurityConfigRepository).save(saltSecurityConfig);
    }

    @Test
    void testCreateConfig() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setAccountId("accountId");
        when(stackService.getStackById(2L)).thenReturn(stack);
        SecurityConfig securityConfig = new SecurityConfig();
        stack.setSecurityConfig(securityConfig);
        when(tlsSecurityService.generateSecurityKeys(stack.getAccountId(), securityConfig)).thenReturn(securityConfig);
        when(securityConfigRepository.save(securityConfig)).thenAnswer(invocation -> invocation.getArgument(0, SecurityConfig.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        underTest.initSaltSecurityConfigs(2L);

        verify(saltSecurityConfigRepository).save(securityConfig.getSaltSecurityConfig());
        verify(securityConfigRepository).save(securityConfig);
    }

    @Test
    void testCreateConfigAlreadyExists() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setAccountId("accountId");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(new SaltSecurityConfig());
        stack.setSecurityConfig(securityConfig);
        when(stackService.getStackById(2L)).thenReturn(stack);

        underTest.initSaltSecurityConfigs(2L);

        verifyNoInteractions(tlsSecurityService, securityConfigRepository);
    }

    @Test
    void testUpdateSeLinuxSecurityConfig() {
        when(securityConfigRepository.updateSeLinuxSecurityConfig(SeLinux.ENFORCING, 1L)).thenReturn(1);
        int result = underTest.updateSeLinuxSecurityConfig(1L, SeLinux.ENFORCING);
        assertEquals(1, result);
    }
}
