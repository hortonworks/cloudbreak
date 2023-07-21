package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
public class ImageComparatorTest {

    private static final String OS_REDHAT8 = "redhat8";

    private static final String OS_CENTOS7 = "centos7";

    private static final String OS_TYPE_REDHAT8 = "redhat8";

    private static final String OS_TYPE_REDHAT7 = "redhat7";

    private ImageComparator underTest;

    @Mock
    private ImageOsService imageOsService;

    @BeforeEach
    public void setup() {
        underTest = new ImageComparator();
        ReflectionTestUtils.setField(underTest, "imageOsService", imageOsService);
        lenient().when(imageOsService.getPreferredOs()).thenReturn(OS_CENTOS7);
    }

    @Test
    public void testGreaterWithDefaultOs() {
        Image image1 = createMockImage(OS_CENTOS7, OS_TYPE_REDHAT7, "2020-06-08", 1589562459L);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        assertEquals(1L, underTest.compare(image1, image2));
    }

    @Test
    public void testEqualsWithSameDefaultOsAndSameTime() {
        Image image1 = createMockImage(OS_CENTOS7, OS_TYPE_REDHAT7, "2020-06-08", 1589562459L);
        Image image2 = createMockImage(OS_CENTOS7, OS_TYPE_REDHAT7, "2020-06-08", 1589562459L);
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testEqualsWithSameNonDefaultOsAndSameTime() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testEqualsWithCreationTime() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testEqualsWithDate() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", null);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", null);
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testGreaterWithCreationTime() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-08", 1589562459L);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-03-12", 1513861351L);
        assertEquals(1L, underTest.compare(image1, image2));
    }

    @Test
    public void testGreaterWithDate() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-24", null);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-15", null);
        assertEquals(1L, underTest.compare(image1, image2));
    }

    @Test
    public void testSmallerWithCreationTime() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-15", 1913560024L);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-24", 1934560145L);
        assertEquals(-1L, underTest.compare(image1, image2));
    }

    @Test
    public void testSmallerWithDate() {
        Image image1 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-15", null);
        Image image2 = createMockImage(OS_REDHAT8, OS_TYPE_REDHAT8, "2020-06-24", null);
        assertEquals(-1L, underTest.compare(image1, image2));
    }

    private static Image createMockImage(String os, String osType, String date, Long created) {
        Image image = mock(Image.class);
        lenient().when(image.getOs()).thenReturn(os);
        lenient().when(image.getOsType()).thenReturn(osType);
        lenient().when(image.getDate()).thenReturn(date);
        lenient().when(image.getCreated()).thenReturn(created);
        return image;
    }
}