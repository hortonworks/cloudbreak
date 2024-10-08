package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.DurationV4Response;

@Component
public class DurationToDurationV4ResponseConverter {

    public DurationV4Response convert(Duration source) {
        DurationV4Response response = new DurationV4Response();
        response.setDays((int) source.toDaysPart());
        response.setHours(source.toHoursPart());
        response.setMinutes(source.toMinutesPart());
        return response;
    }
}
