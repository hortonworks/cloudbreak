package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.ImageUtil;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class CpuArchUpgradeImageFilterTest {

    @Mock
    private ImageUtil imageUtil;

    @InjectMocks
    private CpuArchUpgradeImageFilter underTest;

    @Mock
    private Image image1;

    @Mock
    private Image image2;

    @Mock
    private Image image3;

    @Mock
    private ImageFilterParams imageFilterParams;

    @Test
    public void testAllArmImageIsFilteredOut() {
        when(imageUtil.isArm64Image(image1)).thenReturn(true);
        when(imageUtil.isArm64Image(image2)).thenReturn(true);
        when(imageUtil.isArm64Image(image3)).thenReturn(false);

        ImageFilterResult result = underTest.filter(new ImageFilterResult(List.of(image1, image2, image3)), imageFilterParams);

        assertEquals(List.of(image3), result.getImages());
    }
}