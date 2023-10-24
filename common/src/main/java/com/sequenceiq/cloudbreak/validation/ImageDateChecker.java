package com.sequenceiq.cloudbreak.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDateChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageDateChecker.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd");

    private final String minImageDate;

    private final LocalDate minImageLocalDate;

    public ImageDateChecker(String minImageDate) {
        this.minImageDate = minImageDate;
        this.minImageLocalDate = LocalDate.parse(minImageDate, FORMATTER);
    }

    public String getMinImageDate() {
        return minImageDate;
    }

    public boolean isImageDateValidOrNull(String imageDate) {
        if (imageDate == null) {
            return true;
        } else {
            try {
                LocalDate imageLocalDate = LocalDate.parse(imageDate, FORMATTER);
                return minImageLocalDate.isBefore(imageLocalDate) || minImageLocalDate.isEqual(imageLocalDate);
            } catch (DateTimeParseException e) {
                LOGGER.debug("Parsing image date '{}' was not successful.", imageDate);
                return true;
            }
        }
    }
}
