package com.sequenceiq.it.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class LongStringGeneratorUtil {

    public String stringGenerator(int lenghtOfString) {
        char[] charArray = new char[lenghtOfString];

        Arrays.fill(charArray, 'a');

        return new String(charArray);
    }
}
