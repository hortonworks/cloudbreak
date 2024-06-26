package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Architecture;
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
    private com.sequenceiq.cloudbreak.cloud.model.Image currentImage;

    @Mock
    private Image oldImage;

    @Mock
    private Image x86Image;

    @Mock
    private Image armImage;

    @Mock
    private ImageFilterParams imageFilterParams;

    @BeforeEach
    void setUp() {
        when(imageFilterParams.getCurrentImage()).thenReturn(currentImage);
        when(oldImage.getArchitecture()).thenReturn(null);
        when(x86Image.getArchitecture()).thenReturn(Architecture.X86_64.getName());
        when(armImage.getArchitecture()).thenReturn(Architecture.ARM64.getName());
    }

    @Test
    public void testCurrentImageArchitectureNull() {
        when(currentImage.getArchitecture()).thenReturn(null);

        ImageFilterResult result = underTest.filter(new ImageFilterResult(List.of(armImage, oldImage, x86Image)), imageFilterParams);

        assertEquals(List.of(oldImage, x86Image), result.getImages());
    }

    @Test
    public void testCurrentImageArchitectureX86() {
        when(currentImage.getArchitecture()).thenReturn(Architecture.X86_64.getName());

        ImageFilterResult result = underTest.filter(new ImageFilterResult(List.of(armImage, oldImage, x86Image)), imageFilterParams);

        assertEquals(List.of(oldImage, x86Image), result.getImages());
    }

    @Test
    public void testCurrentImageArchitectureArm64() {
        when(currentImage.getArchitecture()).thenReturn(Architecture.ARM64.getName());

        ImageFilterResult result = underTest.filter(new ImageFilterResult(List.of(armImage, oldImage, x86Image)), imageFilterParams);

        assertEquals(List.of(armImage), result.getImages());
    }
}