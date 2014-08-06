package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;

public interface BlueprintService {
    Set<Blueprint> getAll(User user);

    BlueprintJson get(Long blueprintId);

    void delete(Long blueprintId);

    Blueprint addBlueprint(User user, BlueprintJson blueprintJson);
}
