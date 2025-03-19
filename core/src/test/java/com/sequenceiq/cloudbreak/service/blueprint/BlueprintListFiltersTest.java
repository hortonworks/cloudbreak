package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;

@ExtendWith(MockitoExtension.class)
class BlueprintListFiltersTest {

    @InjectMocks
    private BlueprintListFilters underTest;

    @Spy
    private SupportedRuntimes supportedRuntimes;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(supportedRuntimes, "latestSupportedRuntime", "7.1.0");
    }

    @Test
    void testUserManagedShallBeShown() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.0.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.1.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.2.0", null)));

        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.0.0", FALSE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.1.0", FALSE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.2.0", FALSE)));
    }

    @Test
    void testDefaultManagedShallBeShownBasedOnVersion() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.0.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.1.0", null)));

        assertFalse(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.2.0", null)), "Version is newer than supported");
    }

    @Test
    void testDatalakeShallBeShown() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.0.0", TRUE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.1.0", TRUE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.2.0", TRUE)));
    }

    @Test
    void testIsLakehouseOptimizer() {
        assertTrue(underTest.isLakehouseOptimizer(createBlueprintFile("cloudera_lakehouse_optimizer")));
        assertFalse(underTest.isLakehouseOptimizer(createBlueprintFile("cloudera_tohaz_optimalizalo")));
        assertFalse(underTest.isLakehouseOptimizer(createBlueprintFile("enterprise-datalake")));
    }

    private BlueprintView createBlueprintView(ResourceStatus status, String version, Boolean sdxReady) {
        BlueprintView blueprint = new BlueprintView();
        blueprint.setStatus(status);
        blueprint.setStackVersion(version);
        if (sdxReady != null) {
            blueprint.setTags(Json.silent(Map.of("shared_services_ready", sdxReady)));
        }
        return blueprint;
    }

    private BlueprintFile createBlueprintFile(String stackName) {
        return new BlueprintFile.Builder()
                .name("name")
                .blueprintText("blueprintText")
                .stackName(stackName)
                .stackVersion("stackVersion")
                .stackType("stackType")
                .build();
    }
}
