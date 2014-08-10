package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.TemplateHistory;

@Component
public class AzureTemplateHistoryConverter extends AbstractHistoryConverter<AzureTemplate, TemplateHistory> {

    @Override
    public TemplateHistory convert(AzureTemplate entity) {
        TemplateHistory history = new TemplateHistory();
        history.setEntityId(entity.getId());
        history.setName(entity.getName());
        history.setDescription(entity.getDescription());
        history.setUserId(entity.getOwner().getId());
        //entity.getAzureTemplateOwner();
        history.setImageName(entity.getImageName());
        history.setLocation(entity.getLocation().location());
        //history.entity.getPorts();
        history.setVmType(entity.getVmType());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return AzureTemplate.class.equals(entity.getClass());
    }
}