package com.sequenceiq.provisioning.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariBlueprintService {

    private static final long TEST_CLOUD_ID = 12L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariBlueprintService.class);

    public void addBlueprint(BlueprintJson blueprintJson) {

        // TODO: cloudRepo.findOne(blueprintJson.getCloudId()) .getAmbariHost
        // .getAmbariPort;
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
        if (!ambariClient.addBlueprint(blueprintJson.getAmbariBlueprint())) {
            throw new BadRequestException("Failed to validate Ambari blueprint.");
        }
    }

    public List<BlueprintJson> retrieveBlueprints(User user) {
        // TODO: for cloudRepo.findAllForUser().each() {
        // getBlueprints(cloud.host,cloud.port)}

        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setCloudId(TEST_CLOUD_ID);
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        try {
            JsonParser jp = factory
                    .createParser("{\"host_groups\":[{\"name\":\"master\",\"components\":[{\"name\":\"NAMENODE\"},"
                            + "{\"name\":\"SECONDARY_NAMENODE\"},{\"name\":\"RESOURCEMANAGER\"},{\"name\":\"HISTORYSERVER\"},"
                            + "{\"name\":\"NAGIOS_SERVER\"},{\"name\":\"ZOOKEEPER_SERVER\"}],\"cardinality\":\"1\"},"
                            + "{\"name\":\"slaves\",\"components\":[{\"name\":\"DATANODE\"},{\"name\":\"HDFS_CLIENT\"},"
                            + "{\"name\":\"NODEMANAGER\"},{\"name\":\"YARN_CLIENT\"},{\"name\":\"MAPREDUCE2_CLIENT\"},"
                            + "{\"name\":\"ZOOKEEPER_CLIENT\"}],\"cardinality\":\"2\"}],"
                            + "\"Blueprints\":{\"blueprint_name\":\"multi-node-hdfs-yarn\",\"stack_name\":\"HDP\",\"stack_version\":\"2.0\"}}");
            JsonNode actualObj = mapper.readTree(jp);
            blueprintJson.setAmbariBlueprint(actualObj);
        } catch (JsonParseException e) {
            LOGGER.error("Failed to parse JSON.", e);
        } catch (IOException e) {
            LOGGER.error("Failed to create parser.", e);
        }
        List<BlueprintJson> blueprints = new ArrayList<>();
        blueprints.add(blueprintJson);
        return blueprints;
    }

    public BlueprintJson retrieveBlueprint(User user, String id) {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setCloudId(TEST_CLOUD_ID);
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        try {
            JsonParser jp = factory
                    .createParser("{\"host_groups\":[{\"name\":\"master\",\"components\":[{\"name\":\"NAMENODE\"},"
                            + "{\"name\":\"SECONDARY_NAMENODE\"},{\"name\":\"RESOURCEMANAGER\"},{\"name\":\"HISTORYSERVER\"},"
                            + "{\"name\":\"NAGIOS_SERVER\"},{\"name\":\"ZOOKEEPER_SERVER\"}],\"cardinality\":\"1\"},"
                            + "{\"name\":\"slaves\",\"components\":[{\"name\":\"DATANODE\"},{\"name\":\"HDFS_CLIENT\"},"
                            + "{\"name\":\"NODEMANAGER\"},{\"name\":\"YARN_CLIENT\"},{\"name\":\"MAPREDUCE2_CLIENT\"},"
                            + "{\"name\":\"ZOOKEEPER_CLIENT\"}],\"cardinality\":\"2\"}],"
                            + "\"Blueprints\":{\"blueprint_name\":\"multi-node-hdfs-yarn\",\"stack_name\":\"HDP\",\"stack_version\":\"2.0\"}}");
            JsonNode actualObj = mapper.readTree(jp);
            blueprintJson.setAmbariBlueprint(actualObj);
        } catch (JsonParseException e) {
            LOGGER.error("Failed to parse JSON.", e);
        } catch (IOException e) {
            LOGGER.error("Failed to create parser.", e);
        }
        return blueprintJson;
    }
}
