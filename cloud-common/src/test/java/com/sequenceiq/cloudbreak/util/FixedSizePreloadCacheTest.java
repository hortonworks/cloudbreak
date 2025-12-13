package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class FixedSizePreloadCacheTest {

    @Test
    void testPopWhenOnTheFlyGenerationNeeded() {
        FixedSizePreloadCache<String> cache = new FixedSizePreloadCache<>(0, () -> Thread.currentThread().getName());
        String actual = cache.pop();

        assertEquals(Thread.currentThread().getName(), actual);
    }

    @Test
    void testPopWhenReplacementNeeded() {
        Iterator<String> items = Arrays.asList("a", "b", "c").iterator();
        FixedSizePreloadCache<String> cache = new FixedSizePreloadCache<>(1, items::next);

        assertEquals("a", cache.pop());
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> cache.size() == 1);
        assertEquals("b", cache.pop());
    }
}
