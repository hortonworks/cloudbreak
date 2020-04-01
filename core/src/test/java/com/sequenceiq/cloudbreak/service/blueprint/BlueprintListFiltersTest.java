package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintListFiltersTest {

    @InjectMocks
    private BlueprintListFilters underTest;

    @Spy
    private SupportedRuntimes supportedRuntimes;

    @Before
    public void setup() {
        Whitebox.setInternalState(supportedRuntimes, "latestSupportedRuntime", "7.1.0");
    }

    @Test
    public void testUserManagedShallBeShown() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.0.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.1.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.2.0", null)));

        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.0.0", FALSE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.1.0", FALSE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(USER_MANAGED, "7.2.0", FALSE)));
    }

    @Test
    public void testDefaultManagedShallBeShownBasedOnVersion() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.0.0", null)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.1.0", null)));

        assertFalse("Version is newer than supported", underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.2.0", null)));
    }

    @Test
    public void testDatalakeShallBeShown() {
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.0.0", TRUE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.1.0", TRUE)));
        assertTrue(underTest.isDistroXDisplayed(createBlueprintView(DEFAULT, "7.2.0", TRUE)));
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
}