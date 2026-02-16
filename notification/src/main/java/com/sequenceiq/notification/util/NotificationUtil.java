package com.sequenceiq.notification.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NotificationUtil {

    private NotificationUtil() {
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = Arrays.stream(input.toLowerCase().split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1) + " ")
                .collect(Collectors.joining());
        return result.trim();
    }

}
