package com.sequenceiq.redbeams.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class UserGeneratorService {

    private static final int MAX_RANDOM_INT_FOR_CHARACTER = 26;

    private static final int USER_NAME_LENGTH = 10;

    public String generateUserName() {
        return ThreadLocalRandom.current().ints(0, MAX_RANDOM_INT_FOR_CHARACTER)
                .limit(USER_NAME_LENGTH).boxed()
                .map(i -> Character.toString((char) ('a' + i)))
                .collect(Collectors.joining());
    }
}
