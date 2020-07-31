package com.sequenceiq.datalake.controller.sdx;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

public class SdxUpgradeClusterConverterTest {

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
}