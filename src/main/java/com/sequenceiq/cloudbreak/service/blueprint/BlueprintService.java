package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;

public interface BlueprintService {

    Blueprint addBlueprint(User user, Blueprint blueprint);

    Blueprint get(Long blueprintId);

    Set<Blueprint> getAll(User user);

    void delete(Long blueprintId);

}
