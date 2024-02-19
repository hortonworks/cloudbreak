package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class CentOSToRedHatUpgradeAvailabilityServiceTest {

    private static final String CM_BUILD_NUMBER = "12345";

    @InjectMocks
    private CentOSToRedHatUpgradeAvailabilityService underTest;

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private Map<String, String> stackRelatedParcels;

    @Test
    void testIsOsUpgradePermittedShouldReturnTrue() {
        Image targetImage = createImage(RHEL8);
        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(stackRelatedParcels);
        when(lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER)).thenReturn(true);

        assertTrue(underTest.isOsUpgradePermitted(createModelImage(CENTOS7), targetImage, stack));

        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER);
    }

    @Test
    void testIsOsUpgradePermittedShouldReturnFalseWhenTheUpgradePathIsRedhatToCentOs() {
        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(stackRelatedParcels);

        assertFalse(underTest.isOsUpgradePermitted(createModelImage(RHEL8), createImage(CENTOS7), stack));

        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verifyNoInteractions(lockedComponentChecker);
    }

    @Test
    void testIsOsUpgradePermittedShouldReturnFalseWhenTheComponentVersionsAreNotMatching() {
        Image targetImage = createImage(RHEL8);
        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(stackRelatedParcels);
        when(lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER)).thenReturn(false);

        assertFalse(underTest.isOsUpgradePermitted(createModelImage(CENTOS7), targetImage, stack));

        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER);
    }

    private Image createImage(OsType os) {
        return ImageTestBuilder.builder().withOs(os.getOs()).withOsType(os.getOsType()).build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createModelImage(OsType os) {
        return ModelImageTestBuilder.builder()
                .withOs(os.getOs())
                .withOsType(os.getOsType())
                .withPackageVersions(Map.of(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), CM_BUILD_NUMBER))
                .build();
    }
}