package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Component
public class BlueprintHistoryConverter extends AbstractHistoryConverter<Blueprint, BlueprintHistory> {
    @Override
    public BlueprintHistory convert(Blueprint entity) {
        BlueprintHistory history = new BlueprintHistory();
        history.setEntityId(entity.getId());
        history.setUserId(entity.getUser().getId());
        history.setDescription(entity.getDescription());
        history.setBlueprintName(entity.getBlueprintName());
        history.setBlueprintText(entity.getBlueprintText());
        history.setHostGroupCount(entity.getHostGroupCount());
        history.setName(entity.getName());
        history.setBlueprintDescription(entity.getDescription());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return Blueprint.class.equals(entity.getClass());
    }
}
