package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.HistoryJson;
import com.sequenceiq.periscope.domain.History;

@Component
public class HistoryConverter extends AbstractConverter<HistoryJson, History> {

    @Override
    public HistoryJson convert(History source) {
        HistoryJson json = new HistoryJson();
        json.setId(source.getId());
        json.setAdjustment(source.getAdjustment());
        json.setAdjustmentType(source.getAdjustmentType());
        json.setAlertType(source.getAlertType());
        json.setCbStackId(source.getCbStackId());
        json.setClusterId(source.getClusterId());
        json.setHostGroup(source.getHostGroup());
        json.setOriginalNodeCount(source.getOriginalNodeCount());
        json.setProperties(source.getProperties());
        json.setScalingStatus(source.getScalingStatus());
        json.setTimestamp(source.getTimestamp());
        json.setStatusReason(source.getStatusReason());
        return json;
    }
}
