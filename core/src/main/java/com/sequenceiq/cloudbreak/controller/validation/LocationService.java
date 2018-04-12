package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    public String location(String region, String availabilityZone) {
        return Strings.isNullOrEmpty(availabilityZone) ? region : availabilityZone;
    }
}
