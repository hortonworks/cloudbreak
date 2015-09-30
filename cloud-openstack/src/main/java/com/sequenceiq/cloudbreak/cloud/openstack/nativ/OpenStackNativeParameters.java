package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class OpenStackNativeParameters implements PlatformParameters {

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
        return new HashMap<>();
    }

    @Override
    public String defaultDiskType() {
        return "";
    }

    @Override
    public Map<String, String> regions() {
        return new HashMap<>();
    }

    @Override
    public String defaultRegion() {
        return "";
    }

    @Override
    public Map<String, List<String>> availabiltyZones() {
        return new HashMap<>();
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
