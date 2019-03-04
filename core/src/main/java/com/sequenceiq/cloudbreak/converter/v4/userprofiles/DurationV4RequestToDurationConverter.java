package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.DurationV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class DurationV4RequestToDurationConverter extends AbstractConversionServiceAwareConverter<DurationV4Request, Duration> {

    @Override
    public Duration convert(DurationV4Request source) {
        return Duration
                .ofSeconds(source.getSeconds())
                .plusMinutes(source.getMinutes())
                .plusHours(source.getHours())
                .plusDays(source.getDays());
    }

    private long getValue(Integer value) {
        return value != null ? value.longValue() : 0L;
    }
}
