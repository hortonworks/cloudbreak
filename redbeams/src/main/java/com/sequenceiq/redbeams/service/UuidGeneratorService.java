package com.sequenceiq.redbeams.service;

import com.google.common.annotations.VisibleForTesting;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class UuidGeneratorService {

    private static final String UUID_EXTRACTOR_REGEXP = "(\\w{8})-(\\w{4})-\\w(\\w{3})-\\w(\\w{3})-(\\w{12})";

    private static final Pattern UUID_EXTRACTOR_PATTERN = Pattern.compile(UUID_EXTRACTOR_REGEXP);

    /**
     * Generates a random UUID.
     *
     * @return random UUID
     */
    public String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a random UUID and extracts the variable portions of its string format, up to a
     * maximum length. The non-random parts include the hyphen separators and the initial hex digits
     * of the third and fourth groupings.
     *
     * @param  maxLength maximum length for returned string
     * @return           string based on random UUID
     */
    public String uuidVariableParts(int maxLength) {
        return uuidVariableParts(maxLength, randomUuid());
    }

    @VisibleForTesting
    String uuidVariableParts(int maxLength, String uuid) {
        if (maxLength < 1) {
            return "";
        }
        Matcher matcher = UUID_EXTRACTOR_PATTERN.matcher(uuid);
        if (!matcher.matches()) {
            return StringUtils.left(uuid, maxLength);
        }
        StringBuilder variablePartsBuilder = new StringBuilder();
        IntStream.range(1, matcher.groupCount() + 1).forEach(i -> variablePartsBuilder.append(matcher.group(i)));
        String uuidVariableParts = variablePartsBuilder.toString();
        return StringUtils.left(uuidVariableParts, maxLength);
    }
}
