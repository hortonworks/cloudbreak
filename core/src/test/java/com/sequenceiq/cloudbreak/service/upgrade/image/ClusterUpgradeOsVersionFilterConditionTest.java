package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeOsVersionFilterConditionTest {

    private static final Set<OsType> OS_USED_BY_INSTANCES = Collections.emptySet();

    @Mock
    private OsChangeService osChangeService;

    @InjectMocks
    private ClusterUpgradeOsVersionFilterCondition underTest;

    @ParameterizedTest(name = "[{index}] Upgrade from {0} to target image: {1}, rhel9Enabled: {2}, osChangePermitted: {3}, should be allowed: {4}")
    @MethodSource("provideTestParameters")
    public void testIsImageAllowed(OsType currentOsType, Image image, boolean rhel9Enabled, boolean osChangePermitted, boolean expectedResult) {

        lenient().when(osChangeService.isOsChangePermitted(image, currentOsType, OS_USED_BY_INSTANCES, Architecture.X86_64.getName()))
                .thenReturn(osChangePermitted);

        boolean actual = underTest.isImageAllowed(currentOsType, Architecture.X86_64.getName(), image, rhel9Enabled, OS_USED_BY_INSTANCES);

        assertEquals(expectedResult, actual);
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of(OsType.RHEL8, createTargetImage(OsType.RHEL8), true, true, true),
                Arguments.of(OsType.RHEL8, createTargetImage(OsType.RHEL8), false, true, true),
                Arguments.of(OsType.RHEL8, createTargetImage(OsType.RHEL9), true, true, true),
                Arguments.of(OsType.RHEL8, createTargetImage(OsType.RHEL9), false, true, false),
                Arguments.of(OsType.RHEL8, createTargetImage(OsType.RHEL9), true, false, false),
                Arguments.of(OsType.RHEL9, createTargetImage(OsType.RHEL9), true, false, true)
        );
    }

    private static Image createTargetImage(OsType osType) {
        return Image.builder().withOs(osType.getOs()).withOsType(osType.getOsType()).build();
    }
}