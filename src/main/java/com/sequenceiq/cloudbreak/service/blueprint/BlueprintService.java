package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;

public interface BlueprintService {

    Set<Blueprint> retrievePrivateBlueprints(CbUser user);

    Set<Blueprint> retrieveAccountBlueprints(CbUser user);

    Blueprint get(Long id);

    Blueprint create(CbUser user, Blueprint blueprintRequest);

    void delete(Long id, CbUser user);

    Blueprint getPublicBlueprint(String name, CbUser user);

    Blueprint getPrivateBlueprint(String name, CbUser user);

    void delete(String name, CbUser user);
}
