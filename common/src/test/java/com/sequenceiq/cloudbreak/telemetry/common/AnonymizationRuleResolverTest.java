package com.sequenceiq.cloudbreak.telemetry.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class AnonymizationRuleResolverTest {

    private AnonymizationRuleResolver underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AnonymizationRuleResolver();
    }

    @Test
    public void testDecodeRules() {
        // GIVEN
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule1 = new AnonymizationRule();
        rule1.setReplacement("replace1");
        AnonymizationRule rule2 = new AnonymizationRule();
        rule2.setReplacement("replace2");
        rule2.setValue(Base64Util.encode("value2"));
        rules.add(rule1);
        rules.add(rule2);
        // WHEN
        List<AnonymizationRule> finalRules = underTest.decodeRules(rules);
        // THEN
        assertEquals(finalRules.size(), 1);
        assertEquals(finalRules.get(0).getValue(), "value2");
    }
}
