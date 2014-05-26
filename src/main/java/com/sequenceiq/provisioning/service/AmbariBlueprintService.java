package com.sequenceiq.provisioning.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.converter.BlueprintConverter;
import com.sequenceiq.provisioning.domain.Blueprint;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.BlueprintRepository;

@Service
public class AmbariBlueprintService {

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    public void addBlueprint(User user, BlueprintJson blueprintJson) {
        Blueprint blueprint = blueprintConverter.convert(blueprintJson);
        blueprint.setUser(user);
        blueprintRepository.save(blueprint);
    }

    public Set<BlueprintJson> getAll(User user) {
        return blueprintConverter.convertAllEntityToJson(user.getBlueprints());
    }

    public BlueprintJson get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        return blueprintConverter.convert(blueprint);
    }

    public void delete(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        blueprintRepository.delete(blueprint);
    }
}
