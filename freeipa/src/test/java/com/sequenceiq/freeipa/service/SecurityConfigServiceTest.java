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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
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
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(tlsSecurityService.generateSecurityKeys(stack.getAccountId())).thenReturn(securityConfig);
        when(securityConfigRepository.save(securityConfig)).thenAnswer(invocation -> invocation.getArgument(0, SecurityConfig.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));

        underTest.createIfDoesntExists(2L);

        ArgumentCaptor<Stack> stackArgumentCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService).save(stackArgumentCaptor.capture());
        Stack savedStack = stackArgumentCaptor.getValue();
        assertEquals(stack, savedStack);
        assertEquals(securityConfig, savedStack.getSecurityConfig());
        verify(saltSecurityConfigRepository).save(saltSecurityConfig);
    }

    @Test
    void testCreateConfigAlreadyExists() throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setAccountId("accountId");
        stack.setSecurityConfig(new SecurityConfig());
        when(stackService.getStackById(2L)).thenReturn(stack);

        underTest.createIfDoesntExists(2L);

        verifyNoInteractions(tlsSecurityService, securityConfigRepository);
    }
}
