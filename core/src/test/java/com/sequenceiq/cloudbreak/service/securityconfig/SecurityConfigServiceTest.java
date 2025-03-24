package com.sequenceiq.cloudbreak.service.securityconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SecurityV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.SeLinux;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    @BeforeEach
    public void setUp() throws TransactionService.TransactionExecutionException {
        lenient().when(transactionService.required(any(Supplier.class))).then(invocationOnMock -> ((Supplier) invocationOnMock.getArgument(0)).get());
        stack = new Stack();
        stack.setId(123L);
        stack.setWorkspace(new Workspace());
        stack.setName("stack-name");
    }

    @Test
    void testSecurityConfigExistsAndPermissiveSelinux() {
        // This can happen when the flow is restarted
        SecurityConfig existingSecurityConfig = new SecurityConfig();
        existingSecurityConfig.setSeLinux(SeLinux.PERMISSIVE);
        existingSecurityConfig.setSaltSecurityConfig(new SaltSecurityConfig());
        when(securityConfigRepository.findOneByStackId(anyLong())).thenReturn(Optional.of(existingSecurityConfig));

        SecurityConfig securityConfig = underTest.initSaltSecurityConfigs(stack);

        assertEquals(existingSecurityConfig, securityConfig);
        assertEquals(SeLinux.PERMISSIVE, securityConfig.getSeLinux());
    }

    @Test
    void validateRequestShouldThrowBadRequestExceptionWhenSeLinuxIsEnforcingEntitlementNotGranted() {
        SecurityV4Request mockRequest = mock(SecurityV4Request.class);
        when(mockRequest.getSeLinux()).thenReturn(SeLinux.ENFORCING.name());
        when(entitlementService.isCdpSecurityEnforcingSELinux(any())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateRequest(mockRequest, "accountId"));
        assertEquals("SELinux enforcing requires CDP_SECURITY_ENFORCING_SELINUX entitlement for your account.", exception.getMessage());
    }

    @Test
    void validateRequestShouldNOTThrowBadRequestExceptionWhenSeLinuxIsEnforcingEntitlementGranted() {
        SecurityV4Request mockRequest = mock(SecurityV4Request.class);
        when(mockRequest.getSeLinux()).thenReturn(SeLinux.ENFORCING.name());
        when(entitlementService.isCdpSecurityEnforcingSELinux(any())).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateRequest(mockRequest, "accountId"));
    }

    @Test
    void validateRequestShouldNotThrowExceptionWhenSeLinuxIsPermissive() {
        SecurityV4Request mockRequest = mock(SecurityV4Request.class);
        when(mockRequest.getSeLinux()).thenReturn(SeLinux.PERMISSIVE.name());

        assertDoesNotThrow(() -> underTest.validateRequest(mockRequest, "accountId"));
    }

    @Test
    void validateRequestShouldNotThrowExceptionWhenSecurityRequestIsNull() {
        assertDoesNotThrow(() -> underTest.validateRequest(null, "accountId"));
    }

    @Test
    void testSecurityConfigExistsAndNoSelinux() {
        // This can happen when the flow is restarted
        SecurityConfig existingSecurityConfig = new SecurityConfig();
        existingSecurityConfig.setSeLinux(SeLinux.PERMISSIVE);
        existingSecurityConfig.setSaltSecurityConfig(new SaltSecurityConfig());
        when(securityConfigRepository.findOneByStackId(anyLong())).thenReturn(Optional.of(existingSecurityConfig));

        SecurityConfig securityConfig = underTest.initSaltSecurityConfigs(stack);

        assertEquals(existingSecurityConfig, securityConfig);
        assertEquals(SeLinux.PERMISSIVE, securityConfig.getSeLinux());
    }

    @Test
    void testSecurityConfigDoesNotExists() {
        SecurityConfig createdSecurityConfig = new SecurityConfig();
        when(tlsSecurityService.generateSecurityKeys(any(Workspace.class), any(SecurityConfig.class))).thenReturn(createdSecurityConfig);
        when(securityConfigRepository.save(any(SecurityConfig.class))).then(AdditionalAnswers.returnsFirstArg());

        SecurityConfig securityConfig = underTest.initSaltSecurityConfigs(stack);

        assertEquals(createdSecurityConfig, securityConfig);
    }

    @Test
    void testChangeSaltPassword() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword("old-pw");
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        String newPassword = "new-pw";

        underTest.changeSaltPassword(securityConfig, newPassword);

        verify(saltSecurityConfigService).save(saltSecurityConfig);
        assertEquals(newPassword, saltSecurityConfig.getSaltPassword());
    }

    @Test
    void testUpdateSeLinuxSecurityConfig() {
        when(securityConfigRepository.updateSeLinuxSecurityConfig(SeLinux.ENFORCING, 1L)).thenReturn(1);
        int result = underTest.updateSeLinuxSecurityConfig(1L, SeLinux.ENFORCING);
        assertEquals(1, result);
    }

}
