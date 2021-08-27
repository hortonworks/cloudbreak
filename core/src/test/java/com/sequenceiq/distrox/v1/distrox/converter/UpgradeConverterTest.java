package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

class UpgradeConverterTest {

    private final UpgradeConverter underTest = new UpgradeConverter();

    @ParameterizedTest
    @EnumSource(UpgradeShowAvailableImages.class)
    public void testFromUpgradeShowAvailableImagesToDistroxUpgradeShowAvailableImages(UpgradeShowAvailableImages upgradeShowAvailableImagesEnum) {
        DistroXUpgradeShowAvailableImages.valueOf(upgradeShowAvailableImagesEnum.name());
    }

    @ParameterizedTest
    @EnumSource(DistroXUpgradeShowAvailableImages.class)
    public void testFromDistroxUpgradeShowAvailableImagesToUpgradeShowAvailableImages(DistroXUpgradeShowAvailableImages distroxUpgradeShowAvailableImages) {
        UpgradeShowAvailableImages.valueOf(distroxUpgradeShowAvailableImages.name());
    }

    @Test
    public void testConvertRequest() {
        DistroXUpgradeV1Request source = new DistroXUpgradeV1Request();
        source.setDryRun(Boolean.TRUE);
        source.setImageId("asdf");
        source.setShowAvailableImages(DistroXUpgradeShowAvailableImages.LATEST_ONLY);
        source.setLockComponents(Boolean.TRUE);
        source.setReplaceVms(DistroXUpgradeReplaceVms.DISABLED);
        source.setRuntime("runtime");

        UpgradeV4Request result = underTest.convert(source, new InternalUpgradeSettings(false, true, true));

        assertEquals(source.getDryRun(), result.getDryRun());
        assertEquals(source.getImageId(), result.getImageId());
        assertEquals(source.getShowAvailableImages().name(), result.getShowAvailableImages().name());
        assertEquals(source.getLockComponents(), result.getLockComponents());
        assertEquals(Boolean.FALSE, result.getReplaceVms());
        assertEquals(source.getRuntime(), result.getRuntime());
        assertFalse(result.getInternalUpgradeSettings().isSkipValidations());
    }

    @Test
    public void testConvertRequestWhenInternal() {
        // GIVEN
        DistroXUpgradeV1Request source = new DistroXUpgradeV1Request();
        // WHEN
        UpgradeV4Request result = underTest.convert(source, new InternalUpgradeSettings(true, true, true));
        // THEN
        assertTrue(result.getInternalUpgradeSettings().isSkipValidations());
    }

    @Test
    public void testConvertRequestWhenReplaceVmsParamIsNotGiven() {
        // GIVEN
        DistroXUpgradeV1Request source = new DistroXUpgradeV1Request();
        // WHEN
        UpgradeV4Request result = underTest.convert(source, new InternalUpgradeSettings(false, true, true));
        // THEN
        assertNull(result.getReplaceVms());
    }

    @Test
    public void testConvertResult() {
        UpgradeV4Response source = new UpgradeV4Response();
        source.setUpgradeCandidates(List.of());
        source.setCurrent(new ImageInfoV4Response());
        source.setFlowIdentifier(new FlowIdentifier(FlowType.FLOW, "asdg"));
        source.setReason("fdas");

        DistroXUpgradeV1Response result = underTest.convert(source);

        assertEquals(source.getCurrent(), result.getCurrent());
        assertEquals(source.getFlowIdentifier(), result.getFlowIdentifier());
        assertEquals(source.getReason(), result.getReason());
        assertEquals(source.getUpgradeCandidates(), result.getUpgradeCandidates());
    }
}
