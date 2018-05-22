package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CustomContainerRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemBase;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterAttributes;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class ClusterRequestToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Cluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestToClusterConverter.class);

    @Override
    public Cluster convert(ClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(REQUESTED);
        cluster.setDescription(source.getDescription());
        cluster.setEmailNeeded(source.getEmailNeeded());
        cluster.setUserName(source.getUserName());
        cluster.setPassword(source.getPassword());
        cluster.setExecutorType(source.getExecutorType());
        Boolean enableSecurity = source.getEnableSecurity();
        cluster.setSecure(enableSecurity == null ? Boolean.FALSE : enableSecurity);
        convertGateway(source, cluster);

        if (source.getKerberos() != null) {
            KerberosConfig kerberosConfig = getConversionService().convert(source.getKerberos(), KerberosConfig.class);
            cluster.setKerberosConfig(kerberosConfig);
        }
        cluster.setConfigStrategy(source.getConfigStrategy());
        cluster.setEmailTo(source.getEmailTo());
        FileSystemBase fileSystem = source.getFileSystem();
        cluster.setCloudbreakAmbariPassword(PasswordUtil.generatePassword());
        cluster.setCloudbreakAmbariUser("cloudbreak");
        convertAttributes(source, cluster);
        if (fileSystem != null) {
            cluster.setFileSystem(getConversionService().convert(fileSystem, FileSystem.class));
        }
        try {
            Json json = new Json(convertContainerConfigs(source.getCustomContainer()));
            cluster.setCustomContainerDefinition(json);
        } catch (JsonProcessingException ignored) {
            cluster.setCustomContainerDefinition(null);
        }
        cluster.setAmbariSecurityMasterKey(source.getAmbariSecurityMasterKey());
        return cluster;
    }

    private void convertGateway(ClusterRequest source, Cluster cluster) {
        GatewayJson gatewayJson = source.getGateway();
        if (gatewayJson != null) {
            Gateway gateway = getConversionService().convert(gatewayJson, Gateway.class);
            if (gateway != null) {
                setGatewayPathAndSsoProvider(source, gatewayJson, gateway);
                cluster.setGateway(gateway);
                gateway.setCluster(cluster);
            }
        }
    }

    private void setGatewayPathAndSsoProvider(ClusterRequest source, GatewayJson gatewayJson, Gateway gateway) {
        gateway.setPath(source.getName());
        if (gatewayJson.getPath() != null) {
            gateway.setPath(gatewayJson.getPath());
        }
        if (gateway.getSsoProvider() == null) {
            gateway.setSsoProvider('/' + gateway.getPath() + "/sso/api/v1/websso");
        }
    }

    private void convertAttributes(ClusterRequest source, Cluster cluster) {
        Map<String, Object> attributesMap = source.getCustomQueue() != null
                ? Collections.singletonMap(ClusterAttributes.CUSTOM_QUEUE.name(), source.getCustomQueue())
                : Collections.emptyMap();
        try {
            cluster.setAttributes(new Json(attributesMap));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not initiate the attribute map on cluster object: ", e);
            throw new CloudbreakApiException("Failed to store exposedServices", e);
        }
    }

    private Map<String, String> convertContainerConfigs(CustomContainerRequest customContainerRequest) {
        Map<String, String> configs = new HashMap<>();
        if (customContainerRequest != null) {
            for (Entry<String, String> stringStringEntry : customContainerRequest.getDefinitions().entrySet()) {
                configs.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
        }
        return configs;
    }
}
