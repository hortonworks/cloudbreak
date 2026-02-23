package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PYTHON38;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.CurrentImagePackageProvider;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;

@ExtendWith(MockitoExtension.class)
class PythonVersionBasedRuntimeVersionValidatorTest {

    private static final long STACK_ID = 1L;

    private static final List<Image> CDH_IMAGES_FROM_CATALOG = Collections.emptyList();

    @InjectMocks
    private PythonVersionBasedRuntimeVersionValidator underTest;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private StackDto stack;

    @Mock
    private CurrentImagePackageProvider currentImagePackageProvider;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @ParameterizedTest(name = "Current runtime: {0} contains Python 3.8: {1} current image used on instances: {2}, "
            + "Stack type: {3}, Target runtime: {4} contains Python 3.8: {5}, Os upgrade: {6}, Permitting upgrade: {7}")
    @MethodSource("testScenariosProvider")
    public void test(String currentRuntimeVersion, boolean currentImageContainsPython, boolean allInstanceContainsPython, StackType stackType,
            String targetRuntimeVersion, boolean targetImageContainsPython, boolean osUpgrade, boolean expectedValue) {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createModelImage(currentRuntimeVersion, currentImageContainsPython);
        Image targetImage = createImage(targetRuntimeVersion, targetImageContainsPython);
        lenient().when(lockedComponentService.isComponentsLocked(stack, currentImage, targetImage)).thenReturn(osUpgrade);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getType()).thenReturn(stackType);
        lenient().when(currentImagePackageProvider.currentInstancesContainsPackage(STACK_ID, CDH_IMAGES_FROM_CATALOG, PYTHON38))
                .thenReturn(allInstanceContainsPython);
        lenient().when(currentImageUsageCondition.isCurrentImageUsedOnInstances(STACK_ID, currentImage.getImageId())).thenReturn(currentImageContainsPython);

        assertEquals(expectedValue, underTest.isUpgradePermittedForRuntime(stack, CDH_IMAGES_FROM_CATALOG, currentImage, targetImage));
    }

    private static Object[][] testScenariosProvider() {
        return new Object[][] {
                { "7.2.12", true, true, WORKLOAD, "7.2.12", true, false, true },
                { "7.2.12", true, true, WORKLOAD,  "7.2.12", true, true, true },
                { "7.2.12", false, true, WORKLOAD,  "7.2.12", true, true, true },
                { "7.2.12", false, true, WORKLOAD,  "7.2.12", false, true, true },
                { "7.2.12", true, false, WORKLOAD,  "7.2.12", true, true, true },

                { "7.2.12", true, true, WORKLOAD,  "7.2.15", true, false, true },
                { "7.2.12", false, true, WORKLOAD,  "7.2.15", true, false, true },
                { "7.2.12", true, false, WORKLOAD,  "7.2.15", true, false, true },
                { "7.2.12", true, true, WORKLOAD,  "7.2.15", false, false, true },
                { "7.2.12", true, true, WORKLOAD,  "7.2.15", false, false, true },
                { "7.2.12", false, true, WORKLOAD,  "7.2.15", false, false, true },
                { "7.2.12", false, false, WORKLOAD,  "7.2.15", false, false, true },

                { "7.2.15", true, true, WORKLOAD,  "7.2.16", true, false, true },
                { "7.2.15", false, true, WORKLOAD,  "7.2.16", true, false, false },
                { "7.2.15", true, false, WORKLOAD,  "7.2.16", true, false, false },

                { "7.2.16", true, true, WORKLOAD,  "7.2.16", true, true, true },
                { "7.2.16", true, true, WORKLOAD,  "7.2.16", true, false, true },
                { "7.2.16", false, true, WORKLOAD,  "7.2.16", true, false, false },
                { "7.2.16", true, false, WORKLOAD,  "7.2.16", true, false, false },
                { "7.2.16", true, false, WORKLOAD,  "7.2.16", true, true, true },
                { "7.2.16", false, true, WORKLOAD,  "7.2.16", true, true, true },
                { "7.2.16", false, true, WORKLOAD,  "7.3.1", false, true, false },
                { "7.2.16", true, true, WORKLOAD,  "7.3.2", false, true, true },

                { "7.2.16", true, true, WORKLOAD,  "7.2.17", true, false, true },
                { "7.2.16", false, true, WORKLOAD,  "7.2.17", true, false, false },

                { "7.2.15", true, true, DATALAKE,  "7.2.17", true, false, true },
                { "7.2.15", false, true, DATALAKE,  "7.2.17", true, false, false },
                { "7.2.15", true, false, DATALAKE,  "7.2.17", true, false, false },

                { "7.2.17", true, true, DATALAKE,  "7.2.17", true, true, true },
                { "7.2.17", true, true, DATALAKE,  "7.2.17", true, false, true },
                { "7.2.17", false, true, DATALAKE,  "7.2.17", true, false, false },
                { "7.2.17", true, false, DATALAKE,  "7.2.17", true, false, false },
                { "7.2.17", true, false, DATALAKE,  "7.2.17", true, true, true },
                { "7.2.17", false, true, DATALAKE,  "7.2.17", true, true, true },
                { "7.2.17", true, true, DATALAKE,  "7.3.1", false, true, true },
                { "7.2.17", true, true, DATALAKE,  "7.3.2", false, true, true },

                { "7.2.17", true, true, DATALAKE,  "7.2.18", true, false, true },
                { "7.2.17", false, true, DATALAKE,  "7.2.18", true, false, false },
        };
    }

    private Image createImage(String runtimeVersion, boolean containsPython38) {
        Map<String, String> packageVersions = createPackageVersions(runtimeVersion, containsPython38);
        return Image.builder().withPackageVersions(packageVersions).build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createModelImage(String runtimeVersion, boolean containsPython38) {
        Map<String, String> packageVersions = createPackageVersions(runtimeVersion, containsPython38);
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withPackageVersions(packageVersions).build();
    }

    private Map<String, String> createPackageVersions(String runtimeVersion, boolean containsPython38) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(STACK.getKey(), runtimeVersion);
        if (containsPython38) {
            packageVersions.put(PYTHON38.getKey(), "3.8");
        }
        return packageVersions;
    }
}