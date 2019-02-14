package com.sequenceiq.cloudbreak.clusterdefinition;

import org.springframework.stereotype.Service;

@Service
public class VolumeConfigProvider {

    public String getValue(boolean global, Integer volumeCount, ConfigProperty property, String serviceName) {
        String directory = serviceName.toLowerCase() + (property.getDirectory().isEmpty() ? "" : '/' + property.getDirectory());
        String value = null;
        if (volumeCount == null && global) {
            value = property.getPrefix() + VolumeUtils.getLogVolume(directory);
        } else if (volumeCount != null && volumeCount > 0) {
            value = global ? property.getPrefix() + VolumeUtils.getLogVolume(directory) : VolumeUtils.buildVolumePathString(volumeCount, directory);
        }
        return value;
    }
}
