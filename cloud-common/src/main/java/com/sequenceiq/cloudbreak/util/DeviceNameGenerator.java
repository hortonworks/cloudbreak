package com.sequenceiq.cloudbreak.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceNameGenerator {

    protected static final List<Character> DEVICE_NAME_POSTFIX_LETTER = List.of('b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');

    private final String deviceNameTemplate;

    private final AtomicInteger offset;

    public DeviceNameGenerator(String deviceNameTemplate, int offset) {
        this.deviceNameTemplate = deviceNameTemplate;
        this.offset = new AtomicInteger(offset);
    }

    public String next() {
        int currentLetter = offset.getAndIncrement();
        if (currentLetter > DEVICE_NAME_POSTFIX_LETTER.size() - 1) {
            throw new IllegalStateException("Ran out of device names.");
        }
        return String.format(deviceNameTemplate, DEVICE_NAME_POSTFIX_LETTER.get(currentLetter));
    }
}
