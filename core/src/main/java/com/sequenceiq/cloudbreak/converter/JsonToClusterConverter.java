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
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.json.Json;

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
        cluster.setKerberosMasterKey(source.getKerberosMasterKey());
        cluster.setKerberosAdmin(source.getKerberosAdmin());
        cluster.setKerberosPassword(source.getKerberosPassword());
        cluster.setLdapRequired(source.getLdapRequired());
        cluster.setConfigStrategy(source.getConfigStrategy());
        cluster.setEnableShipyard(source.getEnableShipyard());
        cluster.setEmailTo(source.getEmailTo());
        FileSystemBase fileSystem = source.getFileSystem();
        if (fileSystem != null) {
            cluster.setFileSystem(getConversionService().convert(fileSystem, FileSystem.class));
        }
        try {
            Json json = new Json(convertBlueprintInputJsons(source.getBlueprintInputs()));
            cluster.setBlueprintInputs(source.getBlueprintInputs() == null ? new Json(new HashMap<>()) : json);
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
