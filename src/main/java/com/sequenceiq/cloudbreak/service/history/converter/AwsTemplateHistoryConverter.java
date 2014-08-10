package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.TemplateHistory;

@Component
public class AwsTemplateHistoryConverter extends AbstractHistoryConverter<AwsTemplate, TemplateHistory> {
    @Override
    public TemplateHistory convert(AwsTemplate entity) {
        TemplateHistory history = new TemplateHistory();
        history.setEntityId(entity.getId());
        history.setUserId(entity.getOwner().getId());
        history.setDescription(entity.getDescription());
        history.setAmiid(entity.getAmiId());
        history.setInstancetype(entity.getInstanceType().name());
        history.setName(entity.getName());
        history.setRegion(entity.getRegion().name());
        history.setSshLocation(entity.getSshLocation());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return AwsTemplate.class.equals(entity.getClass());
    }

}
