package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class ImageComparatorTest {

    private ImageComparator underTest;

    @Before
    public void setup() {
        underTest = new ImageComparator();
    }

    @Test
    public void testEqualsWithCreationTime() {
        Image image1 = createMockImage("2020-06-08", Long.valueOf(1589562459));
        Image image2 = createMockImage("2020-06-08", Long.valueOf(1589562459));
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testEqualsWithDate() {
        Image image1 = createMockImage("2020-06-08", null);
        Image image2 = createMockImage("2020-06-08", null);
        assertEquals(0L, underTest.compare(image1, image2));
    }

    @Test
    public void testGreaterWithCreationTime() {
        Image image1 = createMockImage("2020-06-08", Long.valueOf(1589562459));
        Image image2 = createMockImage("2020-03-12", Long.valueOf(1513861351));
        assertEquals(1L, underTest.compare(image1, image2));
    }

    @Test
    public void testGreaterWithDate() {
        Image image1 = createMockImage("2020-06-24", null);
        Image image2 = createMockImage("2020-06-15", null);
        assertEquals(1L, underTest.compare(image1, image2));
    }

    @Test
    public void testSmallerWithCreationTime() {
        Image image1 = createMockImage("2020-06-15", Long.valueOf(1913560024));
        Image image2 = createMockImage("2020-06-24", Long.valueOf(1934560145));
        assertEquals(-1L, underTest.compare(image1, image2));
    }

    @Test
    public void testSmallerWithDate() {
        Image image1 = createMockImage("2020-06-15", null);
        Image image2 = createMockImage("2020-06-24", null);
        assertEquals(-1L, underTest.compare(image1, image2));
    }

    private static Image createMockImage(String date, Long created) {
        Image image = mock(Image.class);
        lenient().when(image.getDate()).thenReturn(date);
        when(image.getCreated()).thenReturn(created);
        return image;
    }
}