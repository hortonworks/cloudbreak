package com.sequenceiq.freeipa.converter.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityRuleRequest;
import com.sequenceiq.freeipa.converter.instance.securityrule.SecurityRuleRequestToSecurityRuleConverter;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;

@ExtendWith(MockitoExtension.class)
class SecurityGroupRequestToSecurityGroupConverterTest {

    @InjectMocks
    private SecurityGroupRequestToSecurityGroupConverter underTest;

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private SecurityRuleRequestToSecurityRuleConverter securityRuleConverter;

    @Test
    void testConvertWithNullSecurityRulesAndNullGroupIds() {
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        securityGroupRequest.setSecurityRules(null);
        securityGroupRequest.setSecurityGroupIds(null);

        SecurityGroup result = underTest.convert(securityGroupRequest);

        assertNotNull(result.getSecurityGroupIds());
        assertTrue(result.getSecurityGroupIds().isEmpty());
        assertNotNull(result.getSecurityRules());
        assertTrue(result.getSecurityRules().isEmpty());
    }

    @Test
    void testConvertWithEmptySecurityRulesAndProvidedGroupIds() {
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        securityGroupRequest.setSecurityGroupIds(Set.of("sg-1", "sg-2"));
        securityGroupRequest.setSecurityRules(List.of());

        SecurityGroup result = underTest.convert(securityGroupRequest);

        assertNotNull(result.getSecurityGroupIds());
        assertEquals(2, result.getSecurityGroupIds().size());
        assertNotNull(result.getSecurityRules());
        assertTrue(result.getSecurityRules().isEmpty());
    }

    @Test
    void testConvertWithSecurityRulesAndEmptyGroupIds() {
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        SecurityRuleRequest ruleRequest = new SecurityRuleRequest();
        ruleRequest.setModifiable(false);
        ruleRequest.setPorts(List.of("22"));
        ruleRequest.setProtocol("tcp");
        ruleRequest.setSubnet("0.0.0.0/0");
        securityGroupRequest.setSecurityRules(List.of(ruleRequest));
        securityGroupRequest.setSecurityGroupIds(Set.of());
        SecurityRule securityRule = new SecurityRule();
        securityRule.setCidr("0.0.0.0/0");
        securityRule.setProtocol("tcp");
        securityRule.setPorts("22");
        securityRule.setModifiable(false);
        when(securityRuleConverter.convert(ruleRequest)).thenReturn(securityRule);

        SecurityGroup result = underTest.convert(securityGroupRequest);

        assertNotNull(result.getSecurityGroupIds());
        assertTrue(result.getSecurityGroupIds().isEmpty());
        assertNotNull(result.getSecurityRules());
        assertEquals(1, result.getSecurityRules().size());
        assertEquals(securityRule, result.getSecurityRules().iterator().next());
    }

}