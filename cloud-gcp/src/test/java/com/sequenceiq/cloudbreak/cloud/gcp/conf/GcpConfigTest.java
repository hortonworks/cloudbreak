package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

public class GcpConfigTest {

    private GcpConfig underTest = new GcpConfig();

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "maxAmount", 1);
        ReflectionTestUtils.setField(underTest, "minKeyLength", 2);
        ReflectionTestUtils.setField(underTest, "maxKeyLength", 3);
        ReflectionTestUtils.setField(underTest, "keyValidator", "apple0");
        ReflectionTestUtils.setField(underTest, "minValueLength", 4);
        ReflectionTestUtils.setField(underTest, "maxValueLength", 5);
        ReflectionTestUtils.setField(underTest, "valueValidator", "apple1");
    }

    @Test
    public void testGetTagSpecification() {
        TagSpecification expected = new TagSpecification(1, 2, 3, "apple0", 4, 5, "apple1");
        assertEquals(expected.getKeyValidator(), underTest.getTagSpecification().getKeyValidator());
        assertEquals(expected.getValueValidator(), underTest.getTagSpecification().getValueValidator());
        assertEquals(expected.getMaxAmount(), underTest.getTagSpecification().getMaxAmount());
        assertEquals(expected.getMaxKeyLength(), underTest.getTagSpecification().getMaxKeyLength());
        assertEquals(expected.getMinKeyLength(), underTest.getTagSpecification().getMinKeyLength());
        assertEquals(expected.getMinValueLength(), underTest.getTagSpecification().getMinValueLength());
        assertEquals(expected.getMaxValueLength(), underTest.getTagSpecification().getMaxValueLength());

    }

    @Test
    public void testJsonFactory() {
        assertEquals(JacksonFactory.getDefaultInstance(), underTest.jsonFactory());
    }

}