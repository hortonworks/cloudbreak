package com.sequenceiq.cloudbreak.service.stack.connector;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.HOSTGROUP;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class UserDataBuilder {

    @Value("${cb.host.addr}")
    private String hostAddress;

    @Value("${cb.ambari.docker.tag:2.0.0-consul}")
    private String ambariDockerTag;

    private Map<CloudPlatform, Map<InstanceGroupType, String>> userDataScripts = new HashMap<>();

    public void setUserDataScripts(Map<CloudPlatform, Map<InstanceGroupType, String>> userDataScripts) {
        this.userDataScripts = userDataScripts;
    }

    @PostConstruct
    public void readUserDataScript() throws IOException {
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            Map<InstanceGroupType, String> temp = new HashMap<>();
            for (InstanceGroupType instanceGroupType : InstanceGroupType.values()) {
                temp.put(instanceGroupType, FileReaderUtils.readFileFromClasspath(String.format("%s-%s-init.sh",
                        cloudPlatform.getInitScriptPrefix(), instanceGroupType.name().toLowerCase())));
            }
            userDataScripts.put(cloudPlatform, temp);
        }
    }

    public String buildUserData(CloudPlatform cloudPlatform, String metadataHash, int consulServers, Map<String, String> parameters, InstanceGroupType type) {
        switch (type) {
            case GATEWAY:
                return buildGatewayTypeUserData(cloudPlatform, metadataHash, consulServers, parameters);
            case HOSTGROUP:
                return buildHostGroupTypeUserData(cloudPlatform, metadataHash, consulServers, parameters);
            default:
                return "";
        }
    }

    private String buildHostGroupTypeUserData(CloudPlatform cloudPlatform, String metadataHash, int consulServers, Map<String, String> parameters) {
        parameters.put("METADATA_ADDRESS", hostAddress);
        parameters.put("METADATA_HASH", metadataHash);
        parameters.put("CONSUL_SERVER_COUNT", "" + consulServers);
        parameters.put("AMBARI_DOCKER_TAG", ambariDockerTag);
        String userDataScript = userDataScripts.get(cloudPlatform).get(HOSTGROUP);
        StringBuilder stringBuilder = new StringBuilder("#!/bin/bash\n");
        for (Entry<String, String> parameter : parameters.entrySet()) {
            stringBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("\n");
        }
        stringBuilder.append("\n").append(userDataScript);
        return stringBuilder.toString();
    }

    private String buildGatewayTypeUserData(CloudPlatform cloudPlatform, String metadataHash, int consulServers, Map<String, String> parameters) {
        parameters.put("METADATA_ADDRESS", hostAddress);
        parameters.put("METADATA_HASH", metadataHash);
        parameters.put("CONSUL_SERVER_COUNT", "" + consulServers);
        parameters.put("AMBARI_DOCKER_TAG", ambariDockerTag);
        String userDataScript = userDataScripts.get(cloudPlatform).get(GATEWAY);
        StringBuilder stringBuilder = new StringBuilder("#!/bin/bash\n");
        for (Entry<String, String> parameter : parameters.entrySet()) {
            stringBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("\n");
        }
        stringBuilder.append("\n").append(userDataScript);
        return stringBuilder.toString();
    }

    protected void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    protected void setAmbariDockerTag(String ambariDockerTag) {
        this.ambariDockerTag = ambariDockerTag;
    }
}
