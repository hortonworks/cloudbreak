package com.sequenceiq.cloudbreak.telemetry;

import static com.sequenceiq.common.model.Architecture.ARM64;
import static com.sequenceiq.common.model.Architecture.X86_64;
import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@ExtendWith(MockitoExtension.class)
class TelemetryRepoConfigurationHolderTest {

    @InjectMocks
    private TelemetryRepoConfigurationHolder underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "platformValues", Map.of(
                CENTOS7, Map.of(X86_64, "centos7"),
                RHEL8, Map.of(X86_64, "redhat8", ARM64, "redhat8arm"),
                RHEL9, Map.of(X86_64, "redhat9", ARM64, "redhat9arm")
        ), true);
        FieldUtils.writeField(underTest, "name", "name", true);
        FieldUtils.writeField(underTest, "baseUrl", "url/%s", true);
        FieldUtils.writeField(underTest, "gpgKey", "key/%s", true);
        FieldUtils.writeField(underTest, "gpgCheck", 1, true);
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeRhel7() {
        TelemetryContext context = new TelemetryContext();
        context.setOsType("redhat7");
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("url/centos7", selected.baseUrl());
        assertEquals("key/centos7", selected.gpgKey());
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeRedhat8() {
        TelemetryContext context = new TelemetryContext();
        context.setOsType("redhat8");
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("url/redhat8", selected.baseUrl());
        assertEquals("key/redhat8", selected.gpgKey());
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeRedhat8AndArchIsArm() {
        TelemetryContext context = new TelemetryContext();
        context.setOsType("redhat8");
        context.setArchitecture("arm64");
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("url/redhat8arm", selected.baseUrl());
        assertEquals("key/redhat8arm", selected.gpgKey());
    }

    @Test
    void selectCorrectRepoConfigWhenOsTypeEmpty() {
        TelemetryContext context = new TelemetryContext();
        TelemetryRepoConfiguration selected = underTest.selectCorrectRepoConfig(context);
        assertEquals("url/centos7", selected.baseUrl());
        assertEquals("key/centos7", selected.gpgKey());
    }
}