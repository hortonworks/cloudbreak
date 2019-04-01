package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

public class FixedSizePreloadCacheTest {

    @Test
    public void testPopWhenOnTheFlyGenerationNeeded() {
        FixedSizePreloadCache<String> cache = new FixedSizePreloadCache<>(0, () -> Thread.currentThread().getName());
        String actual = cache.pop();

        assertEquals(Thread.currentThread().getName(), actual);
    }

    @Test
    public void testPopWhenReplacementNeeded() throws InterruptedException {
        Iterator<String> items = Arrays.asList("a", "b", "c").iterator();
        FixedSizePreloadCache<String> cache = new FixedSizePreloadCache<>(1, items::next);

        assertEquals("a", cache.pop());
        Thread.sleep(5);
        assertEquals("b", cache.pop());
    }
}
