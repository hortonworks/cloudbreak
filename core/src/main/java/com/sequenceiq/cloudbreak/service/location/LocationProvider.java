package com.sequenceiq.cloudbreak.service.location;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Service
public class LocationProvider {

    public Location provide(Stack stack) {
        Map<String, AvailabilityZone> availabilityZones = new HashMap<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            availabilityZones.put(instanceGroup.getGroupName(), availabilityZone(instanceGroup.getAvailabilityZone()));
        }
        return location(region(stack.getRegion()),
                availabilityZones.values().iterator().next(),
                availabilityZones);
    }
}
