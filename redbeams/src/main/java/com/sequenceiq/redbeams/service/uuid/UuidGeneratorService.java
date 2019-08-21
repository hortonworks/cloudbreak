package com.sequenceiq.redbeams.service.uuid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class UuidGeneratorService {

    private static final String UUID_EXTRACTOR_REGEXP = "(\\w{8})-(\\w{4})-\\w(\\w{3})-\\w(\\w{3})-(\\w{12})";

    @Inject
    private UuidService uuidService;

    public String uuidVariableParts(int maxLength) {
        if (maxLength < 1) {
            return "";
        }
        Pattern pattern = Pattern.compile(UUID_EXTRACTOR_REGEXP);
        String uuid = uuidService.randomUuid();
        Matcher matcher = pattern.matcher(uuid);
        if (!matcher.matches()) {
            return uuid.substring(0, maxLength < uuid.length() ? maxLength - 1 : uuid.length());
        }
        StringBuilder variablePartsBuilder = new StringBuilder();
        IntStream.range(1, matcher.groupCount() + 1).forEach(i -> variablePartsBuilder.append(matcher.group(i)));
        String uuidVariableParts = variablePartsBuilder.toString();
        return maxLength < uuidVariableParts.length()
                ? uuidVariableParts.substring(0, maxLength)
                : uuidVariableParts;
    }
}
