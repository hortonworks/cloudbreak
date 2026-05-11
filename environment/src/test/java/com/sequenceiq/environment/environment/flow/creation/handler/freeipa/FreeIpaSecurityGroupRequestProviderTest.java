package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaSecurityGroupRequestProviderTest {

    private static final String CIDR = "172.27.0.0/16";

    private static final String DEFAULT = "default";

    @Mock
    private FreeIpaDefaultSecurityRuleRequestProvider defaultSecurityRuleRequestProvider;

    @Mock
    private FreeIpaHybridSecurityRuleRequestProvider hybridSecurityRuleRequestProvider;

    @InjectMocks
    private FreeIpaSecurityGroupRequestProvider underTest;

    private EnvironmentDto environment;

    @Mock
    private SecurityAccessDto securityAccess;

    @Mock
    private SecurityRuleRequest securityRuleRequest;

    @BeforeEach
    void setUp() {
        environment = new EnvironmentDto();
        environment.setSecurityAccess(securityAccess);
    }

    @Test
    void publicCloudWithCidr() {
        environment.setEnvironmentType(EnvironmentType.PUBLIC_CLOUD);
        when(securityAccess.getCidr()).thenReturn(CIDR);
        when(defaultSecurityRuleRequestProvider.createSecurityRuleRequests(CIDR)).thenReturn(List.of(securityRuleRequest));

        SecurityGroupRequest result = underTest.createSecurityGroupRequest(environment);

        assertThat(result.getSecurityGroupIds()).isEmpty();
        assertThat(result.getSecurityRules()).containsExactly(securityRuleRequest);
        verifyNoInteractions(hybridSecurityRuleRequestProvider);
    }

    @Test
    void hybridCloudWithCidr() {
        environment.setEnvironmentType(EnvironmentType.HYBRID);
        when(securityAccess.getCidr()).thenReturn(CIDR);
        when(hybridSecurityRuleRequestProvider.createSecurityRuleRequests(CIDR)).thenReturn(List.of(securityRuleRequest));

        SecurityGroupRequest result = underTest.createSecurityGroupRequest(environment);

        assertThat(result.getSecurityGroupIds()).isEmpty();
        assertThat(result.getSecurityRules()).containsExactly(securityRuleRequest);
        verifyNoInteractions(defaultSecurityRuleRequestProvider);
    }

    @Test
    void defaultSecurityGroup() {
        when(securityAccess.getDefaultSecurityGroupId()).thenReturn(DEFAULT);

        SecurityGroupRequest result = underTest.createSecurityGroupRequest(environment);

        assertThat(result.getSecurityGroupIds()).containsExactly(DEFAULT);
        assertThat(result.getSecurityRules()).isEmpty();
    }

    @Test
    void noCidrAndNoDefaultSecurityGroup() {
        SecurityGroupRequest result = underTest.createSecurityGroupRequest(environment);

        assertThat(result.getSecurityGroupIds()).isEmpty();
        assertThat(result.getSecurityRules()).isEmpty();
    }

}
