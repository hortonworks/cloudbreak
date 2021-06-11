package com.sequenceiq.environment.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.environment.configuration.telemetry.AccountTelemetryConfig;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.repository.AccountTelemetryRepository;

import jregex.Pattern;

@ExtendWith(MockitoExtension.class)
public class AccountTelemetryServiceTest {

    private AccountTelemetryService underTest;

    @Mock
    private AccountTelemetryRepository accountTelemetryRepository;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @BeforeEach
    public void setUp() {
        underTest = new AccountTelemetryService(accountTelemetryRepository, createAccountTelemetryConfig(), regionAwareCrnGenerator);
    }

    @Test
    public void testRulePattern() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444 and 55555-11111";
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule1 = new AnonymizationRule();
        rule1.setReplacement("[REDACTED]");
        String pattern1 = "\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}";
        String encodedPattern1 = new String(Base64.getEncoder().encode(pattern1.getBytes()));
        rule1.setValue(encodedPattern1);
        AnonymizationRule rule2 = new AnonymizationRule();
        rule2.setReplacement("[REDACTED]");
        String pattern2 = "\\d{5}[^\\w]\\d{5}";
        String encodedPattern2 = new String(Base64.getEncoder().encode(pattern2.getBytes()));
        rule2.setValue(encodedPattern2);
        rules.add(rule1);
        rules.add(rule2);
        // WHEN
        String output = underTest.testRulePatterns(rules, input);
        // THEN
        assertThat(output).isEqualTo("My email card number is: [REDACTED] and [REDACTED]");
    }

    @Test
    public void testRulePatternsNoMatch() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444";
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{8}[^\\w]\\d{8}[^\\w]\\d{8}[^\\w]\\d{8}";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        rules.add(rule);
        String output = underTest.testRulePatterns(rules, input);
        // THEN
        assertEquals(input, output);
    }

    @Test
    public void testRulePatternWithInvalidPattern() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444";
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{8}[^[[";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        rules.add(rule);
        // THEN
        assertThrows(BadRequestException.class, () -> {
            // WHEN
            underTest.testRulePatterns(rules, input);
        });
    }

    @Test
    public void testValidateRules() {
        // GIVEN
        AccountTelemetry telemetry = new AccountTelemetry();
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        rules.add(rule);
        telemetry.setRules(rules);
        // WHEN
        underTest.validateAnonymizationRules(telemetry);
    }

    @Test
    public void testValidateRulesWithoutRules() {
        // GIVEN
        AccountTelemetry telemetry = new AccountTelemetry();
        // WHEN
        underTest.validateAnonymizationRules(telemetry);
    }

    @Test
    public void testValidateRulesWithInvalidRules() {
        // GIVEN
        AccountTelemetry telemetry = new AccountTelemetry();
        List<AnonymizationRule> rules = new ArrayList<>();
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "invalidrule{";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        rules.add(rule);
        telemetry.setRules(rules);
        // THEN
        assertThrows(BadRequestException.class, () -> {
            // WHEN
            underTest.validateAnonymizationRules(telemetry);
        });
    }

    @Test
    public void testDefaultRules() {
        // GIVEN
        // WHEN
        AccountTelemetry result = underTest.createDefaultAccuontTelemetry();
        // THEN
        for (AnonymizationRule rule : result.getRules()) {
            testPatternWithOutput(rule, "str myemail@email.com", "email");
            testPatternWithOutput(rule, "333-44-2222", "XXX-");
            testPatternWithOutput(rule, "card number: 1111-2222-3333-4444", "XXXX-");
            testPatternWithOutput(rule, "- FPW: secret", "FPW");
            testPatternWithOutput(rule, "cdpHashedPassword='{SHA512}abcdef'", "[CDP");
        }
        assertThat(result.getFeatures().getClusterLogsCollection().isEnabled()).isEqualTo(false);
    }

    private void testPatternWithOutput(AnonymizationRule rule, String input, String startsWith) {
        if (rule.getReplacement().startsWith(startsWith)) {
            Pattern p = new Pattern(
                    new String(Base64.getDecoder().decode(rule.getValue().getBytes())));
            boolean found = p.matcher(input).find();
            assertThat(found).isEqualTo(true);
        }
    }

    private AccountTelemetryConfig createAccountTelemetryConfig() {
        AccountTelemetryConfig config = new AccountTelemetryConfig();
        List<AnonymizationRule> defaultRules = new ArrayList<>();
        defaultRules.add(createRule("\\b([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-\\._]*[A-Za-z0-9])@(([A-Za-z0-9]|" +
                "[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])\\.)+([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])\\b", "email@redacted.host"));
        defaultRules.add(createRule("\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}", "XXXX-XXXX-XXXX-XXXX"));
        defaultRules.add(createRule("\\d{3}[^\\w]\\d{2}[^\\w]\\d{4}", "XXX-XX-XXXX"));
        defaultRules.add(createRule("FPW\\:\\s+[\\w|\\W].*", "FPW: [REDACTED]"));
        defaultRules.add(createRule("cdpHashedPassword=.*[']", "[CDP PWD ATTRS REDACTED]"));
        config.setRules(defaultRules);
        return config;
    }

    private AnonymizationRule createRule(String pattern, String replacement) {
        AnonymizationRule rule = new AnonymizationRule();
        rule.setValue(pattern);
        rule.setReplacement(replacement);
        return rule;
    }

}
