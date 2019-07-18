package com.sequenceiq.redbeams.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class UserGeneratorService {

    private static final int MAX_RANDOM_INT_FOR_CHARACTER = 26;

    private static final int USER_NAME_LENGTH = 10;

    private static final String UUID_EXTRACTOR_REGEXP = "(\\w{8})-(\\w{4})-\\w(\\w{3})-\\w(\\w{3})-(\\w{12})";

    private static final int MAX_LENGTH_PASSWORD = 30;

    @Inject
    private UUIDGeneratorService uuidGeneratorService;

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
        Pattern pattern = Pattern.compile(UUID_EXTRACTOR_REGEXP);
        String uuid = uuidGeneratorService.randomUuid();
        Matcher matcher = pattern.matcher(uuid);
        if (!matcher.matches()) {
            return uuid.substring(0, MAX_LENGTH_PASSWORD < uuid.length() ? MAX_LENGTH_PASSWORD - 1 : uuid.length());
        }
        StringBuilder passwordBuilder = new StringBuilder();
        IntStream.range(1, matcher.groupCount() + 1).forEach(i -> passwordBuilder.append(matcher.group(i)));
        return passwordBuilder.toString();
    }
}
