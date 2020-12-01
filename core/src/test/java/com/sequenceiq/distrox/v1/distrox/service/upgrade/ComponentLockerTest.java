package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages.LATEST_ONLY;
import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages.SHOW;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Request;

@ExtendWith(MockitoExtension.class)
class ComponentLockerTest {

    private static final String USER_CRN = "userCrn";

    @Mock
    private DistroxUpgradeAvailabilityService upgradeAvailabilityService;

    @InjectMocks
    private ComponentLocker underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][]{
                // testCaseName                                runtimeUpgradeEnable  dryRun   runtime  imageId    lockComponents show          shouldLockComponents
                { "don't lock when runtime upgrade enabled",   TRUE,                 FALSE,   null,    null,      FALSE,         null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 FALSE,   null,    null,      TRUE,          null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    null,    null,      FALSE,         null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 FALSE,   null,    null,      FALSE,         null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    null,    null,      FALSE,         null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    null,    null,      TRUE,          null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    null,    null,      TRUE,          null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    "aa",    "bb",      TRUE,          null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    "aa",    null,      TRUE,          null,         FALSE },
                { "don't lock when runtime upgrade enabled",   TRUE,                 TRUE,    null,    "bb",      TRUE,          null,         FALSE },
                { "lock when show only",                       FALSE,                FALSE,   null,    null,      FALSE,         SHOW,         TRUE  },
                { "lock when latest only",                     FALSE,                FALSE,   null,    null,      FALSE,         LATEST_ONLY,  TRUE  },
                { "dont' lock when show only and locked",      FALSE,                FALSE,   null,    null,      TRUE,          SHOW,         FALSE },
                { "dont'lock when latest only and locked",     FALSE,                FALSE,   null,    null,      TRUE,          LATEST_ONLY,  FALSE },
                { "lock when show only and req empty",         FALSE,                FALSE,   null,    null,      FALSE,         SHOW,         TRUE  },
                { "lock when latest only and req empty",       FALSE,                FALSE,   null,    null,      FALSE,         LATEST_ONLY,  TRUE  },
                { "lock when show only and req empty + dry",   FALSE,                TRUE,    null,    null,      FALSE,         SHOW,         TRUE  },
                { "lock when latest only and req empty + dry", FALSE,                TRUE,    null,    null,      FALSE,         LATEST_ONLY,  TRUE  },
                { "dont lock when runtime defined",            FALSE,                TRUE,    "aa",    null,      FALSE,         SHOW,         FALSE },
                { "dont lock when runtime defined",            FALSE,                TRUE,    "aa",    null,      FALSE,         LATEST_ONLY,  FALSE },
                { "dont lock when imageid defined",            FALSE,                TRUE,    null,    "bb",      FALSE,         SHOW,         FALSE },
                { "dont lock when imageid defined",            FALSE,                TRUE,    null,    "bb",      FALSE,         LATEST_ONLY,  FALSE },
                { "dont lock when runtime defined",            FALSE,                FALSE,   "aa",    null,      FALSE,         SHOW,         FALSE },
                { "dont lock when runtime defined",            FALSE,                FALSE,   "aa",    null,      FALSE,         LATEST_ONLY,  FALSE },
                { "dont lock when imageid defined",            FALSE,                FALSE,   null,    "bb",      FALSE,         SHOW,         FALSE },
                { "dont lock when imageid defined",            FALSE,                FALSE,   null,    "bb",      FALSE,         LATEST_ONLY,  FALSE },
                { "dont lock when runtime defined",            FALSE,                TRUE,    "aa",    null,      FALSE,         null,         FALSE },
                { "dont lock when runtime defined",            FALSE,                TRUE,    "aa",    null,      FALSE,         null,         FALSE },
                { "dont lock when imageid defined",            FALSE,                TRUE,    null,    "bb",      FALSE,         null,         FALSE },
                { "dont lock when imageid defined",            FALSE,                TRUE,    null,    "bb",      FALSE,         null,         FALSE },
                { "dont lock when runtime defined",            FALSE,                FALSE,   "aa",    null,      FALSE,         null,         FALSE },
                { "dont lock when runtime defined",            FALSE,                FALSE,   "aa",    null,      FALSE,         null,         FALSE },
                { "dont lock when imageid defined",            FALSE,                FALSE,   null,    "bb",      FALSE,         null,         FALSE },
                { "dont lock when imageid defined",            FALSE,                FALSE,   null,    "bb",      FALSE,         null,         FALSE },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testLocking(String name, Boolean runtimeUpgradeEnable, Boolean dryRun, String runtime, String imageId,
            Boolean lockComponents, DistroxUpgradeShowAvailableImages showAvailableImages,  Boolean shouldLockComponents) {
        when(upgradeAvailabilityService.isRuntimeUpgradeEnabled(USER_CRN)).thenReturn(runtimeUpgradeEnable);
        DistroxUpgradeV1Request request = new DistroxUpgradeV1Request();
        request.setDryRun(dryRun);
        request.setRuntime(runtime);
        request.setImageId(imageId);
        request.setLockComponents(lockComponents);
        request.setShowAvailableImages(showAvailableImages);

        underTest.lockComponentsIfRuntimeUpgradeIsDisabled(request, USER_CRN, "dontCare");


        if (shouldLockComponents) {
            assertEquals(TRUE, request.getLockComponents());
        } else {
            assertEquals(lockComponents, request.getLockComponents());
        }
    }
}