package com.sequenceiq.cloudbreak.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@ExtendWith(MockitoExtension.class)
class TelemetryRepoConfigurationHolderTest {

    @InjectMocks
    private TelemetryRepoConfigurationHolder underTest;

    @Mock
    private TelemetryRepoConfiguration rhel7;

    @Mock
    private TelemetryRepoConfiguration rhel8;

    @Test
    void selectCorrectRepoConfigWhenOsTypeRhel7() {
        given(rhel7.name()).willReturn("cdp-infra-tools-rhel7");
        TelemetryContext context = new TelemetryContext();
        context.setOsType("redhat7");
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("cdp-infra-tools-rhel7", selected.name());
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeRedhat8() {
        given(rhel8.name()).willReturn("cdp-infra-tools-rhel8");
        TelemetryContext context = new TelemetryContext();
        context.setOsType("redhat8");
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("cdp-infra-tools-rhel8", selected.name());
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeEmpty() {
        given(rhel7.name()).willReturn("cdp-infra-tools-rhel7");
        TelemetryContext context = new TelemetryContext();
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("cdp-infra-tools-rhel7", selected.name());
    }
}