package com.sequenceiq.cloudbreak.cloud.azure.service;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;

@Service("AzureResourceNameService")
public class AzureResourceNameService extends CloudbreakResourceNameService {

    @Value("${cb.max.azure.resource.name.length:}")
    private int maxResourceNameLength;

    public String volumeSet(String stackName, String groupName, Long privateId, String hash) {
        String name = instance(stackName, groupName, privateId);
        name = appendHash(name, hash);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    public String attachedDisk(String stackName, String groupName, Long privateId, int count, String hash) {
        String name = instance(stackName, groupName, privateId);
        name = appendPart(name, count);
        name = appendHash(name, hash);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }

    private String instance(String stackName, String groupName, Long privateId) {
        String name;
        String instanceGroupName = WordUtils.initials(String.valueOf(groupName).replaceAll("_", " "));
        name = normalize(stackName);
        name = adjustPartLength(name);
        name = appendPart(name, normalize(instanceGroupName));
        name = appendPart(name, privateId);
        name = adjustBaseLength(name, maxResourceNameLength);
        return name;
    }
}
