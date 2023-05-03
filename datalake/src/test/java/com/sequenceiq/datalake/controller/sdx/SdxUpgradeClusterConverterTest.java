package com.sequenceiq.datalake.controller.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

public class SdxUpgradeClusterConverterTest {

    private static final String IMAGE_ID = "image-id";

    private static final String RUNTIME = "7.2.16";

    private final SdxUpgradeClusterConverter underTest = new SdxUpgradeClusterConverter();

    @ParameterizedTest
    @EnumSource(UpgradeShowAvailableImages.class)
    public void testFromUpgradeShowAvailableImagesToSdxUpgradeShowAvailableImages(UpgradeShowAvailableImages upgradeShowAvailableImagesEnum) {
        SdxUpgradeShowAvailableImages.valueOf(upgradeShowAvailableImagesEnum.name());
    }

    @ParameterizedTest
    @EnumSource(SdxUpgradeShowAvailableImages.class)
    public void testFromSdxUpgradeShowAvailableImagesToUpgradeShowAvailableImages(SdxUpgradeShowAvailableImages sdxUpgradeShowAvailableImagesEnum) {
        UpgradeShowAvailableImages.valueOf(sdxUpgradeShowAvailableImagesEnum.name());
    }

    @Test
    void testSdxUpgradeRequestToUpgradeV4Request() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(RUNTIME);
        sdxUpgradeRequest.setDryRun(Boolean.TRUE);
        sdxUpgradeRequest.setLockComponents(Boolean.TRUE);
        sdxUpgradeRequest.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);
        sdxUpgradeRequest.setReplaceVms(SdxUpgradeReplaceVms.ENABLED);
        sdxUpgradeRequest.setSkipDataHubValidation(Boolean.TRUE);
        sdxUpgradeRequest.setRollingUpgradeEnabled(Boolean.TRUE);

        UpgradeV4Request actual = underTest.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest);

        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(RUNTIME, actual.getRuntime());
        assertTrue(actual.isDryRun());
        assertTrue(actual.getLockComponents());
        assertEquals(UpgradeShowAvailableImages.LATEST_ONLY, actual.getShowAvailableImages());
        assertTrue(actual.getReplaceVms());
        assertTrue(actual.isSkipDataHubValidation());
    }

    @Test
    void testSdxUpgradeRequestToUpgradeV4RequestShowAvailableImagesShouldBeDoNotShowWhenNullInTheRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(RUNTIME);
        sdxUpgradeRequest.setDryRun(Boolean.TRUE);
        sdxUpgradeRequest.setLockComponents(Boolean.TRUE);
        sdxUpgradeRequest.setReplaceVms(SdxUpgradeReplaceVms.ENABLED);
        sdxUpgradeRequest.setSkipDataHubValidation(Boolean.TRUE);
        sdxUpgradeRequest.setRollingUpgradeEnabled(Boolean.TRUE);

        UpgradeV4Request actual = underTest.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest);

        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(RUNTIME, actual.getRuntime());
        assertTrue(actual.isDryRun());
        assertTrue(actual.getLockComponents());
        assertEquals(UpgradeShowAvailableImages.DO_NOT_SHOW, actual.getShowAvailableImages());
        assertTrue(actual.getReplaceVms());
        assertTrue(actual.isSkipDataHubValidation());
    }

    @Test
    void testSdxUpgradeRequestToUpgradeV4RequestSkipDataHubValidationShouldBeTrueWhenTheRollinUpgradeIsEnabledAndTheSkipDataHubValidationIsNullInTheRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(RUNTIME);
        sdxUpgradeRequest.setDryRun(Boolean.TRUE);
        sdxUpgradeRequest.setLockComponents(Boolean.TRUE);
        sdxUpgradeRequest.setReplaceVms(SdxUpgradeReplaceVms.ENABLED);
        sdxUpgradeRequest.setRollingUpgradeEnabled(Boolean.TRUE);

        UpgradeV4Request actual = underTest.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest);

        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(RUNTIME, actual.getRuntime());
        assertTrue(actual.isDryRun());
        assertTrue(actual.getLockComponents());
        assertEquals(UpgradeShowAvailableImages.DO_NOT_SHOW, actual.getShowAvailableImages());
        assertTrue(actual.getReplaceVms());
        assertTrue(actual.isSkipDataHubValidation());
    }

    @Test
    void testSdxUpgradeRequestToUpgradeV4RequestSkipDataHubValidationShouldBeTrueWhenTheRollinUpgradeIsNullAndTheSkipDataHubValidationIsTrueInTheRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(RUNTIME);
        sdxUpgradeRequest.setDryRun(Boolean.TRUE);
        sdxUpgradeRequest.setLockComponents(Boolean.TRUE);
        sdxUpgradeRequest.setReplaceVms(SdxUpgradeReplaceVms.ENABLED);
        sdxUpgradeRequest.setSkipDataHubValidation(Boolean.TRUE);

        UpgradeV4Request actual = underTest.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest);

        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(RUNTIME, actual.getRuntime());
        assertTrue(actual.isDryRun());
        assertTrue(actual.getLockComponents());
        assertEquals(UpgradeShowAvailableImages.DO_NOT_SHOW, actual.getShowAvailableImages());
        assertTrue(actual.getReplaceVms());
        assertTrue(actual.isSkipDataHubValidation());
    }

    @Test
    void testSdxUpgradeRequestToUpgradeV4RequestSkipDataHubValidationShouldBeFalseWhenTheRollinUpgradeIsNullAndTheSkipDataHubValidationIsNullInTheRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(RUNTIME);
        sdxUpgradeRequest.setDryRun(Boolean.TRUE);
        sdxUpgradeRequest.setLockComponents(Boolean.TRUE);
        sdxUpgradeRequest.setReplaceVms(SdxUpgradeReplaceVms.ENABLED);

        UpgradeV4Request actual = underTest.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest);

        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(RUNTIME, actual.getRuntime());
        assertTrue(actual.isDryRun());
        assertTrue(actual.getLockComponents());
        assertEquals(UpgradeShowAvailableImages.DO_NOT_SHOW, actual.getShowAvailableImages());
        assertTrue(actual.getReplaceVms());
        assertFalse(actual.isSkipDataHubValidation());
    }
}