package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class ClouderaClusterTemplateGenerator {

    public String generateClusterTemplate(String masterFQDN, Cluster cluster,
            Map<String, List<Map<String, String>>> hostGroupMappings, Set<RDSConfig> rdsConfigs) throws IOException {
        ObjectNode hostTemplateJson = (ObjectNode) JsonUtil.readTree(cluster.getClusterDefinition().getClusterDefinitionText());
        Map<String, String> hosts = new HashMap<>();
        hostGroupMappings.forEach((hostGroup, hostAttributes) -> hostAttributes.forEach(
                attr -> hosts.put(attr.get(ClouderaHostGroupAssociationBuilder.FQDN), hostGroup)));
        ObjectNode instantiator = hostTemplateJson.putObject("instantiator");
        instantiator.put("clusterName", cluster.getName());
        ArrayNode hostArray = instantiator.putArray("hosts");
        hosts.forEach((host, hostGroup) -> hostArray.addObject().put("hostName", host).put("hostTemplateRefName", hostGroup));
        Optional<RDSConfig> rdsConfigOptional = rdsConfigs.stream().filter(rds -> DatabaseType.HIVE.name().equalsIgnoreCase(rds.getType())).findFirst();
        if (rdsConfigOptional.isPresent()) {
            RDSConfig hiveRds = rdsConfigOptional.get();
            ArrayNode variables = instantiator.putArray("variables");
            variables.addObject().put("name", "hive-hive_metastore_database_host").put("value", masterFQDN);
            variables.addObject().put("name", "hive-hive_metastore_database_port").put("value", "5432");
            variables.addObject().put("name", "hive-hive_metastore_database_name").put("value", "hive");
            variables.addObject().put("name", "hive-hive_metastore_database_type").put("value", "postgresql");
            variables.addObject().put("name", "hive-hive_metastore_database_password").put("value", hiveRds.getConnectionPassword());
        }
        return hostTemplateJson.toString();
    }
}
