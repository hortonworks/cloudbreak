package com.sequenceiq.cloudbreak.cloud.model.instance;

import org.springframework.stereotype.Service;

@Service
public class AvailabilitySetNameService {

    public String generateName(String prefix, String instanceGroupName) {
        return String.format("%s-%s-as", prefix, instanceGroupName);
    }
}
