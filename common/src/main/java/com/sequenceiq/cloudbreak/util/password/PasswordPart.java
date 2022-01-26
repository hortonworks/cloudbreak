package com.sequenceiq.cloudbreak.util.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordPart {

    private final CharSet charSet;

    private final int charSetLength;

    private final int length;

    public PasswordPart(CharSet charSet, int length) {
        this.charSet = charSet;
        charSetLength = charSet.getValues().size();
        this.length = length;
    }

    public List<Character> generateRandomCharacters(SecureRandom random) {
        List<Character> result = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            result.add(charSet.getValues().get(random.nextInt(charSetLength)));
        }
        return result;
    }
}
