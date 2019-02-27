package com.sequenceiq.cloudbreak.domain;

import java.time.Duration;

import javax.persistence.AttributeConverter;

import org.springframework.stereotype.Component;

@Component
public class DurationToLongConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.toMillis();
    }

    @Override
    public Duration convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }
        return Duration.ofMillis(dbData);
    }
}
