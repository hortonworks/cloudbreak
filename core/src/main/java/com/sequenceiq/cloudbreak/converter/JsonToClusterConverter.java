package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.CustomContainerRequest;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.FileSystemBase;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ExposedServices;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class JsonToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Cluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToClusterConverter.class);

    @Override
    public Cluster convert(ClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(REQUESTED);
        cluster.setDescription(source.getDescription());
        cluster.setEmailNeeded(source.getEmailNeeded());
        cluster.setUserName(source.getUserName());
        cluster.setPassword(source.getPassword());
        Boolean enableSecurity = source.getEnableSecurity();
        cluster.setSecure(enableSecurity == null ? false : enableSecurity);
        convertKnox(source, cluster);
        KerberosRequest kerberos = source.getKerberos();
        KerberosConfig kerberosConfig = new KerberosConfig();
        if (source.getKerberos() != null) {
            kerberosConfig.setKerberosMasterKey(kerberos.getMasterKey());
            kerberosConfig.setKerberosAdmin(kerberos.getAdmin());
            kerberosConfig.setKerberosPassword(kerberos.getPassword());
            kerberosConfig.setKerberosUrl(kerberos.getUrl());
            kerberosConfig.setKerberosRealm(kerberos.getRealm());
            kerberosConfig.setKerberosTcpAllowed(kerberos.getTcpAllowed());
            kerberosConfig.setKerberosPrincipal(kerberos.getPrincipal());
            kerberosConfig.setKerberosLdapUrl(kerberos.getLdapUrl());
            kerberosConfig.setKerberosContainerDn(kerberos.getContainerDn());
        }
        cluster.setKerberosConfig(kerberosConfig);
        cluster.setLdapRequired(source.getLdapRequired());
        cluster.setConfigStrategy(source.getConfigStrategy());
        cluster.setEnableShipyard(source.getEnableShipyard());
        cluster.setEmailTo(source.getEmailTo());
        FileSystemBase fileSystem = source.getFileSystem();
        cluster.setCloudbreakAmbariPassword(PasswordUtil.generatePassword());
        cluster.setCloudbreakAmbariUser("cloudbreak");
        if (fileSystem != null) {
            cluster.setFileSystem(getConversionService().convert(fileSystem, FileSystem.class));
        }
        try {
            Json json = new Json(convertBlueprintInputJsons(source.getBlueprintInputs()));
            cluster.setBlueprintInputs(source.getBlueprintInputs() == null ? new Json(new HashMap<>()) : json);
            if (source.getBlueprintCustomProperties() != null) {
                cluster.setBlueprintCustomProperties(source.getBlueprintCustomProperties());
            } else {
                cluster.setBlueprintCustomProperties(null);
            }
        } catch (JsonProcessingException e) {
            cluster.setBlueprintInputs(null);
        }
        try {
            Json json = new Json(convertContainerConfigs(source.getCustomContainer()));
            cluster.setCustomContainerDefinition(json);
        } catch (JsonProcessingException e) {
            cluster.setCustomContainerDefinition(null);
        }
        return cluster;
    }

    private void convertKnox(ClusterRequest source, Cluster cluster) {
        GatewayJson gatewayJson = source.getGateway();
        Gateway gateway = new Gateway();
        gateway.setEnableGateway(Boolean.FALSE);
        gateway.setTopologyName(source.getName());
        if (source.getGateway().getGatewayType() != null) {
            gateway.setGatewayType(source.getGateway().getGatewayType());
        }
        if (source.getGateway().getSsoProvider() != null) {
            gateway.setSsoProvider(source.getGateway().getSsoProvider());
        }

        ExposedServices exposedServices = new ExposedServices();
        if (gatewayJson != null && gatewayJson.getEnableGateway() != null) {
            gateway.setEnableGateway(gatewayJson.getEnableGateway());
            if (!Strings.isNullOrEmpty(gatewayJson.getTopologyName())) {
                gateway.setTopologyName(gatewayJson.getTopologyName());
            }
            if (gatewayJson.getExposedServices() != null) {
                if (gatewayJson.getExposedServices().contains(ExposedService.ALL.name())) {
                    exposedServices.setServices(ExposedService.getAllKnoxExposed());
                } else {
                    exposedServices.setServices(gatewayJson.getExposedServices());
                }
            }
        }
        try {
            gateway.setExposedServices(new Json(exposedServices));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to store exposedServices", e);
            throw new CloudbreakApiException("Failed to store exposedServices", e);
        }

        gateway.setPath("gateway");
        cluster.setGateway(gateway);
        gateway.setCluster(cluster);
    }

    private Map<String, String> convertBlueprintInputJsons(Set<BlueprintInputJson> inputs) {
        Map<String, String> blueprintInputs = new HashMap<>();
        for (BlueprintInputJson input : inputs) {
            blueprintInputs.put(input.getName(), input.getPropertyValue());
        }
        return blueprintInputs;
    }

    private Map<String, String> convertContainerConfigs(CustomContainerRequest customContainerRequest) {
        Map<String, String> configs = new HashMap<>();
        if (customContainerRequest != null) {
            for (Map.Entry<String, String> stringStringEntry : customContainerRequest.getDefinitions().entrySet()) {
                configs.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
        }
        return configs;
    }
}
