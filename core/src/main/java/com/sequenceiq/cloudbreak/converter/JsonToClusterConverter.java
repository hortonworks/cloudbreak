package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemBase;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class JsonToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Cluster> {
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
        KerberosRequest kerberos = source.getKerberos();
        KerberosConfig kerberosConfig = new KerberosConfig();
        if (source.getKerberos() != null) {
            kerberosConfig.setKerberosMasterKey(kerberos.getMasterKey());
            kerberosConfig.setKerberosAdmin(kerberos.getAdmin());
            kerberosConfig.setKerberosPassword(kerberos.getPassword());
            kerberosConfig.setKerberosUrl(kerberos.getUrl());
            kerberosConfig.setKerberosRealm(kerberos.getRealm());
            kerberosConfig.setKerberosDomain(kerberos.getDomain());
            kerberosConfig.setKerberosPrincipal(kerberos.getPrincipal());
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

    private Map<String, String> convertBlueprintInputJsons(Set<BlueprintInputJson> inputs) {
        Map<String, String> blueprintInputs = new HashMap<>();
        for (BlueprintInputJson input : inputs) {
            blueprintInputs.put(input.getName(), input.getPropertyValue());
        }
        return blueprintInputs;
    }
}
