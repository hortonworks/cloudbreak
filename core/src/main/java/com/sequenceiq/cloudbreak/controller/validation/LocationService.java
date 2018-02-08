package com.sequenceiq.cloudbreak.controller.validation;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class LocationService {

    public String location(String region, String availabilityZone) {
        return Strings.isNullOrEmpty(availabilityZone) ? region : availabilityZone;
    }
}
