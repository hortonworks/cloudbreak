package com.sequenceiq.provisioning.service;

import java.util.List;

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

    public List<BlueprintJson> retrieveBlueprints(User user) {
        return user.get
    }

    public BlueprintJson retrieveBlueprint(User user, String id) {

    }
}
