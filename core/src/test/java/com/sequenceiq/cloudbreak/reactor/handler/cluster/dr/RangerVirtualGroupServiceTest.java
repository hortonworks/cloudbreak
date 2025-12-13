package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.util.TestConstants;

@ExtendWith(MockitoExtension.class)
class RangerVirtualGroupServiceTest {
    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @InjectMocks
    private final RangerVirtualGroupService underTest = new RangerVirtualGroupService();

    @Test
    void getRangerVirtualGroupTestFailed() {
        when(ldapConfigService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(environmentConfigProvider.getParentEnvironmentCrn(anyString())).thenReturn(TestConstants.CRN);
        Stack stack = new Stack();
        stack.setEnvironmentCrn(TestConstants.CRN);
        stack.setName("datalake-stack");
        assertThrows(CloudbreakServiceException.class, () -> underTest.getRangerVirtualGroup(stack));
    }

    @Test
    void getRangerVirtualGroupTestSuccess() {
        when(virtualGroupService.createOrGetVirtualGroup(any(), any()))
                .thenReturn("_c_environments_adminranger");
        LdapView ldapView = LdapView.LdapViewBuilder.aLdapView().withProtocol("")
                .withAdminGroup("admin<>")
                .build();
        when(ldapConfigService.get(anyString(), anyString())).thenReturn(Optional.of(ldapView));
        when(environmentConfigProvider.getParentEnvironmentCrn(anyString())).thenReturn(TestConstants.CRN);
        Stack stack = new Stack();
        stack.setEnvironmentCrn(TestConstants.CRN);
        stack.setName("datalake-stack");
        assertEquals("_c_environments_adminranger", underTest.getRangerVirtualGroup(stack));
    }
}

