package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class OpenStackParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);

    @Override
    public String diskPrefix() {
        return "vd";
    }

    @Override
    public Integer startLabel() {
        return START_LABEL;
    }

    @Override
    public Map<String, String> diskTypes() {
        Map<String, String> disk = new HashMap<>();
        disk.put("HDD", "HDD");
        return disk;
    }

    @Override
    public String defaultDiskType() {
        return diskTypes().get("HDD");
    }

    @Override
    public Map<String, String> regions() {
        Map<String, String> regions = new HashMap<>();
        regions.put("local", "local");
        return regions;
    }

    @Override
    public String defaultRegion() {
        return regions().get("local");
    }

    @Override
    public Map<String, List<String>> availabiltyZones() {
        Map<String, List<String>> availabiltyZones = new HashMap<>();
        availabiltyZones.put("local", new ArrayList<String>());
        return availabiltyZones;
    }

    @Override
    public Map<String, String> virtualMachines() {
        return new HashMap<>();
    }

    @Override
    public String defaultVirtualMachine() {
        return "";
    }
}
