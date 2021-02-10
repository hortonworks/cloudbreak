package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

public class DeviceNameGeneratorTest {

    private static final String DEVICE_TEMPLATE = "/dev/%s";

    @Test
    @DisplayName("test if it can generate all the device letters")
    public void testNextForAllLetters() {
        List<String> expectedDeviceList = List.of(
                "/dev/b", "/dev/c", "/dev/d", "/dev/e",
                "/dev/f", "/dev/g", "/dev/h", "/dev/i",
                "/dev/j", "/dev/k", "/dev/l", "/dev/m",
                "/dev/n", "/dev/o", "/dev/p", "/dev/q",
                "/dev/r", "/dev/s", "/dev/t", "/dev/u",
                "/dev/v", "/dev/w", "/dev/x", "/dev/y", "/dev/z");
        DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_TEMPLATE, 0);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < DeviceNameGenerator.DEVICE_NAME_POSTFIX_LETTER.size(); i++) {
            result.add(generator.next());
        }
        assertArrayEquals(expectedDeviceList.toArray(), result.toArray());
    }

    @Test
    @DisplayName("test if it can generate all the device letters with offset")
    public void testNextForAllLettersWithOffset() {
        List<String> expectedDeviceList = List.of(
                "/dev/f", "/dev/g", "/dev/h", "/dev/i",
                "/dev/j", "/dev/k", "/dev/l", "/dev/m",
                "/dev/n", "/dev/o", "/dev/p", "/dev/q",
                "/dev/r", "/dev/s", "/dev/t", "/dev/u",
                "/dev/v", "/dev/w", "/dev/x", "/dev/y", "/dev/z");
        int offset = 4;
        DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_TEMPLATE, offset);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < DeviceNameGenerator.DEVICE_NAME_POSTFIX_LETTER.size() - offset; i++) {
            result.add(generator.next());
        }
        assertArrayEquals(expectedDeviceList.toArray(), result.toArray());
    }

    @Test(expected = IllegalStateException.class)
    @DisplayName("test if it can handle more iteration than allowed")
    public void testNextForInvalidIteration() {
        int offset = 4;
        DeviceNameGenerator generator = new DeviceNameGenerator(DEVICE_TEMPLATE, offset);
        for (int i = 0; i < DeviceNameGenerator.DEVICE_NAME_POSTFIX_LETTER.size(); i++) {
            generator.next();
        }
    }
}