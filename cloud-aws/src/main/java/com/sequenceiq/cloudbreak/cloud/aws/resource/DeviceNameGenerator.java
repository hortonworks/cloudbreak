package com.sequenceiq.cloudbreak.cloud.aws.resource;

import java.util.Iterator;
import java.util.List;

public class DeviceNameGenerator {

    private static final String DEVICE_NAME_TEMPLATE = "/dev/xvd%s";

    private static final List<Character> DEVICE_NAME_POSTFIX_LETTER = List.of('b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');

//    private static final List<Integer> DEVICE_NAME_POSTFIX_DIGIT = List.of(1, 2, 3, 4, 5, 6);

    private final Iterator<Character> letterIterator;

//    private final Iterator<Integer> digitIterator;

    private Character currentLetter;

    public DeviceNameGenerator() {
        letterIterator = DEVICE_NAME_POSTFIX_LETTER.iterator();

//        digitIterator = DEVICE_NAME_POSTFIX_DIGIT.iterator();
    }

    public String next() {
        if (letterIterator.hasNext()) {
            currentLetter = letterIterator.next();
        } else {
            throw new AwsResourceException("Ran out of device names.");
        }

        return String.format(DEVICE_NAME_TEMPLATE, currentLetter);
    }

//    public String next() {
//        if (!digitIterator.hasNext()) {
//            digitIterator = DEVICE_NAME_POSTFIX_DIGIT.iterator();
//            if (letterIterator.hasNext()) {
//                currentLetter = letterIterator.next();
//            } else {
//                throw new AwsResourceException("Ran out of device names.");
//            }
//        }
//
//        return String.format(DEVICE_NAME_TEMPLATE, currentLetter, digitIterator.next());
//    }
}
