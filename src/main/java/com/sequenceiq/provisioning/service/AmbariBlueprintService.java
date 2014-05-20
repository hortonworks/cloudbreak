package com.sequenceiq.provisioning.service;

import groovyx.net.http.HttpResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariBlueprintService {

    @Autowired
    private JsonHelper jsonHelper;

    public void addBlueprint(User user, Long cloudId, BlueprintJson blueprintJson) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
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
            AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
            Set<String> blueprintNames = ambariClient.getBlueprintsMap().keySet();
            for (String blueprintName : blueprintNames) {
                blueprints.add(createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(blueprintName)));
            }
            return blueprints;
        } catch (IOException e) {
            throw new InternalServerException("Jackson failed to parse blueprint JSON.", e);
        }
    }

    public BlueprintJson retrieveBlueprint(User user, Long cloudId, String id) {
        // TODO get ambari client host and port from cloud service
        AmbariClient ambariClient = new AmbariClient("172.17.0.2", "8080");
        try {
            return createBlueprintJsonFromString(ambariClient.getBlueprintAsJson(id));
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

    private BlueprintJson createBlueprintJsonFromString(String blueprint) throws IOException {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(blueprint));
        return blueprintJson;
    }
}
