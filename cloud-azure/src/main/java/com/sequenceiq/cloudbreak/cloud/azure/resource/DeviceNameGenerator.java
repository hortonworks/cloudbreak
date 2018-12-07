package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.Iterator;
import java.util.List;

public class DeviceNameGenerator {

    private static final String DEVICE_NAME_TEMPLATE = "/dev/sd%s";

    private static final List<Character> DEVICE_NAME_POSTFIX_LETTER = List.of('c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');

    private final Iterator<Character> letterIterator;

    public DeviceNameGenerator() {
        letterIterator = DEVICE_NAME_POSTFIX_LETTER.iterator();
    }

    public String next() {
        Character currentLetter;
        if (letterIterator.hasNext()) {
            currentLetter = letterIterator.next();

        } else {
            throw new AzureResourceException("Ran out of device names.");
        }

        return String.format(DEVICE_NAME_TEMPLATE, currentLetter);
    }
}