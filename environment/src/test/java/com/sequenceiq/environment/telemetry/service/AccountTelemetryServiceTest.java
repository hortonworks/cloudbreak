package com.sequenceiq.environment.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.repository.AccountTelemetryRepository;

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
            // TODO: for now, it can work with java regex - use jregex in the future
            Pattern p = Pattern.compile(
                    new String(Base64.getDecoder().decode(rule.getValue().getBytes())));
            boolean found = p.matcher(input).find();
            assertThat(found).isEqualTo(true);
        }
    }

}
