package com.sequenceiq.cloudbreak.util;

import java.util.Iterator;
import java.util.List;

public class DeviceNameGenerator {

    private static final List<Character> DEVICE_NAME_POSTFIX_LETTER = List.of('b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');

    private String deviceNameTemplate;

    private final Iterator<Character> letterIterator;

    public DeviceNameGenerator(String deviceNameTemplate) {
        this.deviceNameTemplate = deviceNameTemplate;
        letterIterator = DEVICE_NAME_POSTFIX_LETTER.iterator();
    }

    public String next() {
        if (!letterIterator.hasNext()) {
            throw new IllegalStateException("Ran out of device names.");
        }
        return String.format(deviceNameTemplate, letterIterator.next());
    }
}
