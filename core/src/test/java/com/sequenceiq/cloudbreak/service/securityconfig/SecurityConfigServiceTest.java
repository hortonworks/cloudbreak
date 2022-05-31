package com.sequenceiq.cloudbreak.service.securityconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigServiceTest {

    @InjectMocks
    private final SecurityConfigService underTest = new SecurityConfigService();

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private SaltSecurityConfigService saltSecurityConfigService;

    private Stack stack;

    @Before
    public void setUp() throws TransactionService.TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).then(invocationOnMock -> ((Supplier) invocationOnMock.getArgument(0)).get());
        stack = new Stack();
        stack.setId(123L);
        stack.setWorkspace(new Workspace());
        stack.setName("stack-name");
    }

    @Test
    public void testSecurityConfigExists() {
        // This can happen when the flow is restarted
        SecurityConfig existingSecurityConfig = new SecurityConfig();
        when(securityConfigRepository.findOneByStackId(anyLong())).thenReturn(Optional.of(existingSecurityConfig));

        SecurityConfig securityConfig = underTest.generateAndSaveSecurityConfig(stack);

        Assert.assertEquals("It should return the exsiting SecurityConfig", existingSecurityConfig, securityConfig);
    }

    @Test
    public void testSecurityConfigDoesNotExists() {
        SecurityConfig createdSecurityConfig = new SecurityConfig();
        when(tlsSecurityService.generateSecurityKeys(any(Workspace.class))).thenReturn(createdSecurityConfig);
        when(securityConfigRepository.save(any(SecurityConfig.class))).then(AdditionalAnswers.returnsFirstArg());

        SecurityConfig securityConfig = underTest.generateAndSaveSecurityConfig(stack);

        Assert.assertEquals("It should create a new SecurityConfig", createdSecurityConfig, securityConfig);
    }

    @Test
    public void testChangeSaltPassword() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword("old-pw");
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        String newPassword = "new-pw";

        underTest.changeSaltPassword(securityConfig, newPassword);

        verify(saltSecurityConfigService).save(saltSecurityConfig);
        Assert.assertEquals(newPassword, saltSecurityConfig.getSaltPassword());
    }

}
