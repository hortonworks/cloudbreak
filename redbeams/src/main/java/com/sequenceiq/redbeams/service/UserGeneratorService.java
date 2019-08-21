package com.sequenceiq.redbeams.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.service.uuid.UuidGeneratorService;

@Service
public class UserGeneratorService {

    private static final int MAX_RANDOM_INT_FOR_CHARACTER = 26;

    private static final int USER_NAME_LENGTH = 10;

    private static final int PASSWORD_MAX_LENGTH = 30;

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    public String generateUserName() {
        return ThreadLocalRandom.current().ints(0, MAX_RANDOM_INT_FOR_CHARACTER)
                .limit(USER_NAME_LENGTH).boxed()
                .map(i -> Character.toString((char) ('a' + i)))
                .collect(Collectors.joining());
    }

    /*
    Generates a password using a UUID.
    Provider specific limitations:
    - AWS: password length in cf template can be 30 characters long at most
     */
    public String generatePassword() {
        return uuidGeneratorService.uuidVariableParts(PASSWORD_MAX_LENGTH);
    }
}
