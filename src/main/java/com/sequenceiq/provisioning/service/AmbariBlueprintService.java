package com.sequenceiq.provisioning.service;

import groovyx.net.http.HttpResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariBlueprintService {

    public void addBlueprint(User user, Long cloudId, BlueprintJson blueprintJson) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
        try {
            ambariClient.addBlueprint(blueprintJson.getAmbariBlueprint());
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if ("Bad Request".equals(e.getMessage())) {
                throw new BadRequestException("Failed to validate Ambari blueprint.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    public List<BlueprintJson> retrieveBlueprints(User user, Long cloudId) {
        try {
            List<BlueprintJson> blueprints = new ArrayList<>();
            // TODO get ambari client host and port from cloud service
            AmbariClient ambariClient = new AmbariClient("localhost", "49163");
            Set<String> blueprintNames = ambariClient.getBlueprintsMap().keySet();
            for (String blueprintName : blueprintNames) {
                blueprints.add(createBlueprintJsonFromString(cloudId, ambariClient.getBlueprintAsJson(blueprintName)));
            }
            return blueprints;
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    public BlueprintJson retrieveBlueprint(User user, Long cloudId, String id) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("localhost", "49163");
        try {
            return createBlueprintJsonFromString(cloudId, ambariClient.getBlueprintAsJson(id));
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    private BlueprintJson createBlueprintJsonFromString(Long cloudId, String blueprint) throws IOException {
        BlueprintJson blueprintJson = new BlueprintJson();
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser jp = factory.createParser(blueprint);
        JsonNode actualObj = mapper.readTree(jp);
        blueprintJson.setAmbariBlueprint(actualObj);
        return blueprintJson;
    }
}
