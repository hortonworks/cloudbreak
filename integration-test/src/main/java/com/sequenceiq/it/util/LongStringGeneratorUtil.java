package com.sequenceiq.it.util;

import java.util.Arrays;

import org.springframework.stereotype.Component;

@Component
public class LongStringGeneratorUtil {

    public String stringGenerator(int lenghtOfString) {
        char[] charArray = new char[lenghtOfString];

        Arrays.fill(charArray, 'a');

        return new String(charArray);
    }
}
