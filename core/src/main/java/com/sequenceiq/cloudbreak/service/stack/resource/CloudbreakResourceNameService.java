package com.sequenceiq.cloudbreak.service.stack.resource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public abstract class CloudbreakResourceNameService implements ResourceNameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakResourceNameService.class);
    private static final String DELIMITER = "-";
    private static final int MAX_PART_LENGTH = 20;

    protected void checkArgs(int argCnt, Object... parts) {
        if (null == parts && parts.length != argCnt) {
            throw new IllegalStateException("No suitable name parts provided to generate resource name!");
        }
    }

    protected String trimHash(String part) {
        if (part == null) {
            throw new IllegalStateException("Resource name part must not be null!");
        }
        LOGGER.debug("Trim the hash part from the end: {}", part);
        String[] parts = part.split(DELIMITER);
        String trimmed = part;

        if (parts != null) {
            try {
                long ts = Long.valueOf(parts[parts.length - 1]);
                trimmed = StringUtils.collectionToDelimitedString(Arrays.asList(Arrays.copyOf(parts, parts.length - 1)), DELIMITER);
            } catch (NumberFormatException nfe) {
                LOGGER.debug("No need to trim hash: {}", part);
            }
        } else {
            LOGGER.debug("Single part, not hashed: {}", trimmed);
        }
        return trimmed;
    }

    protected String adjustPartLength(String part) {
        if (part == null) {
            throw new IllegalStateException("Resource name part must not be null!");
        }
        String shortPart = part;
        if (part.length() > MAX_PART_LENGTH) {
            LOGGER.debug("Shortening part name: {}", part);
            shortPart = String.copyValueOf(part.toCharArray(), 0, MAX_PART_LENGTH);
        } else {
            LOGGER.debug("Part name length OK, no need to shorten: {}", part);
        }
        return shortPart;
    }

    protected String normalize(String part) {
        if (part == null) {
            throw new IllegalStateException("Resource name part must not be null!");
        }
        LOGGER.debug("Normalizing resource name part: {}", part);
        String normalized = part == null ? "null" : part;

        normalized = StringUtils.trimAllWhitespace(normalized);
        LOGGER.debug("Trimmed whitespaces: {}", part);

        normalized = normalized.replaceAll("[^a-zA-Z0-9]", "");
        LOGGER.debug("Trimmed invalid characters: {}", part);

        normalized = normalized.toLowerCase();
        LOGGER.debug("Lower case: {}", part);

        return normalized;
    }

    protected String appendPart(String base, String part) {
        if (null == base && null == part) {
            throw new IllegalArgumentException("base and part are both null! Can't append them!");
        }
        StringBuilder sb = null;
        if (null != base) {
            sb = new StringBuilder(base).append(DELIMITER).append(part);
        } else {
            sb = new StringBuilder(part);
        }
        return sb.toString();
    }

    protected String appendHash(String name, Date timestamp) {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return new StringBuilder(name).append(DELIMITER).append(df.format(timestamp)).toString();
    }

    protected abstract int getMaxResourceLength();
}


