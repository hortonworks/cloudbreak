package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterdefinition.ConfigProperty;
import com.sequenceiq.cloudbreak.clusterdefinition.VolumeUtils;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HadoopConfigurationUtils {

    public HostgroupView findHostGroupForNode(Collection<HostgroupView> hostGroups, String hostGroupName) {
        return hostGroups.stream()
                .filter(hostGroup -> hostGroup.getName().equals(hostGroupName))
                .findFirst()
                .orElseThrow(() -> new ClusterDefinitionProcessingException(
                        String.format("Couldn't find a saved hostgroup for [%s] hostgroup name in the validation.", hostGroupName)));
    }

    public String getValue(ConfigProperty property, String serviceName, boolean global, int volumeCount) {
        String directory = getDirectory(serviceName, property);

        String value = null;
        if (volumeCount > 0 || (volumeCount == 0 && global)) {
            value = global ? property.getPrefix() + VolumeUtils.getLogVolume(directory) : VolumeUtils.buildVolumePathString(volumeCount, directory);
        }
        return value;
    }

    private String getDirectory(String serviceName, ConfigProperty property) {
        return serviceName.toLowerCase() + (property.getDirectory().isEmpty() ? "" : '/' + property.getDirectory());
    }
}
