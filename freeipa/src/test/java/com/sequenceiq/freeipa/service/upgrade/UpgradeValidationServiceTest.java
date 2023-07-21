package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.ImageInfoResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.image.SupportedOsService;

@ExtendWith(MockitoExtension.class)
class UpgradeValidationServiceTest {

    public static final String ACCOUNT_ID = "accId";

    @InjectMocks
    private UpgradeValidationService underTest;

    @Mock
    private SupportedOsService supportedOsService;

    @Test
    void testMajorOsUpgradeNotSupported() {
        FreeIpaUpgradeRequest upgradeRequest = new FreeIpaUpgradeRequest();
        upgradeRequest.setAllowMajorOsUpgrade(true);
        when(supportedOsService.isRhel8Supported()).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateUpgradeRequest(upgradeRequest));
        assertEquals("Major OS upgrade is not supported", exception.getMessage());
    }

    @Test
    void testOsNotSupported() {
        FreeIpaUpgradeRequest upgradeRequest = new FreeIpaUpgradeRequest();
        upgradeRequest.setAllowMajorOsUpgrade(true);
        ImageSettingsRequest image = new ImageSettingsRequest();
        image.setOs("redhat8");
        upgradeRequest.setImage(image);
        when(supportedOsService.isRhel8Supported()).thenReturn(true);
        when(supportedOsService.isSupported(image.getOs())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateUpgradeRequest(upgradeRequest));
        assertEquals("Selected os 'redhat8' is not supported", exception.getMessage());
    }

    @Test
    public void testStackValidationOk() {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(Boolean.TRUE);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"));

        underTest.validateStackForUpgrade(allInstances, stack);

        allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"), createAvailableInstance("im3"));
        underTest.validateStackForUpgrade(allInstances, stack);

        allInstances = Set.of(createAvailableInstance("im1"));
        underTest.validateStackForUpgrade(allInstances, stack);
    }

    @Test
    public void testNoInstances() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = Set.of();

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testMoreThanThreeInstances() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"), createAvailableInstance("im3"),
                createAvailableInstance("im4"));

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testNotAvailableInstances() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im2 = createAvailableInstance("im2");
        im2.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), im2);

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testStackNotAvailable() {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(Boolean.FALSE);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"));

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    public InstanceMetaData createAvailableInstance(String id) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(id);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    @Test
    public void testCurrentAndSelectedImageAreTheSame() {
        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("111-222");
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        selectedImage.setId("111-222");

        assertThrows(BadRequestException.class, () -> underTest.validateSelectedImageDifferentFromCurrent(currentImage, selectedImage, Set.of()));
    }

    @Test
    public void testCurrentAndSelectedImageAreTheSameThereAreInstancesOnOldImage() {
        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("111-222");
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        selectedImage.setId("111-222");

        underTest.validateSelectedImageDifferentFromCurrent(currentImage, selectedImage, Set.of("a"));
    }

    @Test
    public void testCurrentAndSelectedImageAreDifferent() {
        ImageInfoResponse currentImage = new ImageInfoResponse();
        currentImage.setId("111-222");
        ImageInfoResponse selectedImage = new ImageInfoResponse();
        selectedImage.setId("111-333");

        underTest.validateSelectedImageDifferentFromCurrent(currentImage, selectedImage, Set.of());
    }
}