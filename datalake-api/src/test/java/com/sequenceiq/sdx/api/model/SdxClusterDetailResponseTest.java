package com.sequenceiq.sdx.api.model;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;

class SdxClusterDetailResponseTest {

    private static final String TAG_KEY = "sometthing";

    private SdxClusterDetailResponse underTest;

    @BeforeEach
    void setUp() {
        underTest = new SdxClusterDetailResponse();
    }

    @Test
    void testWhenInternalStackV4ResponseIsNullAndTheInputKeyIsAlsoNullThenNullShouldReturn() {
        String result = underTest.getTagValue(null);

        assertNull(result);
    }

    @Test
    void testWhenInternalStackV4ResponseIsNullAndTheInputKeyIsNotNullThenNullShouldReturn() {
        String result = underTest.getTagValue(TAG_KEY);

        assertNull(result);
    }

    @Test
    void testWhenInternalStackV4ResponseIsNotNullAndTheInputKeyIsNotNullThenNullShouldReturn() {
        underTest = SdxClusterDetailResponse.create(new SdxClusterDetailResponse(), new StackV4Response());
        String result = underTest.getTagValue(TAG_KEY);

        assertNull(result);
    }

    @Test
    void testWhenInternalStackV4ResponseIsNotNullAndTheInputKeyIsNullThenNullShouldReturn() {
        underTest = SdxClusterDetailResponse.create(new SdxClusterDetailResponse(), new StackV4Response());
        String result = underTest.getTagValue(null);

        assertNull(result);
    }

}