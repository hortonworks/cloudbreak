package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class UpgradeConverterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private UpgradeConverter underTest;

    @Mock
    private AccountIdService accountIdService;

    @Mock
    private EntitlementService entitlementService;

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

        UpgradeV4Request result = underTest.convert(source, USER_CRN, false);

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
        UpgradeV4Request result = underTest.convert(source, USER_CRN, true);
        // THEN
        assertTrue(result.getInternalUpgradeSettings().isSkipValidations());
    }

    @Test
    public void testConvertRequestWhenReplaceVmsParamIsNotGiven() {
        // GIVEN
        DistroXUpgradeV1Request source = new DistroXUpgradeV1Request();
        // WHEN
        UpgradeV4Request result = underTest.convert(source, USER_CRN, false);
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

        assertEquals(source.getCurrent(), result.current());
        assertEquals(source.getFlowIdentifier(), result.flowIdentifier());
        assertEquals(source.getReason(), result.reason());
        assertEquals(source.getUpgradeCandidates(), result.upgradeCandidates());
    }

    @Test
    public void testConvertUpgradeCcmResult() {
        StackCcmUpgradeV4Response source = new StackCcmUpgradeV4Response();
        source.setResponseType(CcmUpgradeResponseType.TRIGGERED);
        source.setResourceCrn("crn");
        source.setFlowIdentifier(new FlowIdentifier(FlowType.FLOW, "asdg"));
        source.setReason("fdas");

        DistroXCcmUpgradeV1Response result = underTest.convert(source);

        assertEquals(source.getFlowIdentifier(), result.getFlowIdentifier());
        assertEquals(source.getReason(), result.getReason());
        assertEquals(source.getResourceCrn(), result.getResourceCrn());
        assertEquals(source.getResponseType(), result.getResponseType());
    }
}
