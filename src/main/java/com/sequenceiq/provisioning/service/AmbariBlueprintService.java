package com.sequenceiq.provisioning.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.converter.BlueprintConverter;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariBlueprintService {

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private BlueprintConverter blueprintConverter;

    public void addBlueprint(User user, BlueprintJson blueprintJson) {

    }

    public Set<BlueprintJson> retrieveBlueprints(User user) {
        return blueprintConverter.convertAllEntityToJson(user.getBlueprints());
    }

    public BlueprintJson retrieveBlueprint(User user, String id) {

    }
}
