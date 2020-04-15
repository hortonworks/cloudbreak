package com.sequenceiq.environment.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.repository.AccountTelemetryRepository;

import jregex.Pattern;

@ExtendWith(MockitoExtension.class)
public class AccountTelemetryServiceTest {

    private AccountTelemetryService underTest;

    @Mock
    private AccountTelemetryRepository accountTelemetryRepository;

    @BeforeEach
    public void setUp() {
        underTest = new AccountTelemetryService(accountTelemetryRepository);
    }

    @Test
    public void testRulePattern() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444";
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        // WHEN
        String output = underTest.testRulePattern(rule, input);
        // THEN
        assertThat(output).isEqualTo("My email card number is: [REDACTED]");
    }

    @Test
    public void testRulePatternNoMatch() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444";
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{8}[^\\w]\\d{8}[^\\w]\\d{8}[^\\w]\\d{8}";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        // THEN
        assertThrows(NotFoundException.class, () -> {
            // WHEN
            underTest.testRulePattern(rule, input);
        });
    }

    @Test
    public void testRulePatternWithInvalidPattern() {
        // GIVEN
        String input = "My email card number is: 1111-2222-3333-4444";
        AnonymizationRule rule = new AnonymizationRule();
        rule.setReplacement("[REDACTED]");
        String pattern = "\\d{8}[^[[";
        String encodedPattern = new String(Base64.getEncoder().encode(pattern.getBytes()));
        rule.setValue(encodedPattern);
        // THEN
        assertThrows(BadRequestException.class, () -> {
            // WHEN
            underTest.testRulePattern(rule, input);
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

}
