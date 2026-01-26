package com.sequenceiq.cloudbreak.util;

import java.util.concurrent.atomic.AtomicInteger;

public class IndexingDeviceNameGenerator {
    private static final int DEFAULT_MAX_INDEX = 63;

    private final String deviceNameTemplate;

    private final AtomicInteger offset;

    private final int maxIndex;

    public IndexingDeviceNameGenerator(String deviceNameTemplate, int offset) {
        this(deviceNameTemplate, offset, DEFAULT_MAX_INDEX);
    }

    public IndexingDeviceNameGenerator(String deviceNameTemplate, int offset, int maxIndex) {
        this.deviceNameTemplate = deviceNameTemplate;
        this.offset = new AtomicInteger(offset);
        this.maxIndex = maxIndex;
    }

    public String next() {
        int currentIndex = offset.getAndIncrement();
        if (currentIndex > maxIndex) {
            throw new IllegalStateException("Ran out of device indices.");
        }
        return String.format(deviceNameTemplate, currentIndex);
    }
}