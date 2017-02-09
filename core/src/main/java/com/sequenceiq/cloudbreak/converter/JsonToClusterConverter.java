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
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.FileSystemBase;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ExposedServices;
import com.sequenceiq.cloudbreak.domain.FileSystem;
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
        return cluster;
    }

    private void convertKnox(ClusterRequest source, Cluster cluster) {
        cluster.setEnableKnoxGateway(source.getEnableKnoxGateway());
        if (Strings.isNullOrEmpty(source.getKnoxTopologyName())) {
            cluster.setKnoxTopologyName(source.getName());
        } else {
            cluster.setKnoxTopologyName(source.getKnoxTopologyName());
        }
        try {
            ExposedServices exposedServices = new ExposedServices();
            if (source.getExposedKnoxServices() != null) {
                if (source.getExposedKnoxServices().contains(ExposedService.ALL.name())) {
                    exposedServices.setServices(ExposedService.getAllKnoxExposed());
                } else {
                    exposedServices.setServices(source.getExposedKnoxServices());
                }
            }
            cluster.setExposedKnoxServices(new Json(exposedServices));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to store exposedServices", e);
            throw new CloudbreakApiException("Failed to store exposedServices", e);
        }
    }

    private Map<String, String> convertBlueprintInputJsons(Set<BlueprintInputJson> inputs) {
        Map<String, String> blueprintInputs = new HashMap<>();
        for (BlueprintInputJson input : inputs) {
            blueprintInputs.put(input.getName(), input.getPropertyValue());
        }
        return blueprintInputs;
    }
}
