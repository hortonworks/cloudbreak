package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.domain.History;

@Component
public class HistoryConverter extends AbstractConverter<AutoscaleClusterHistoryResponse, History> {

    @Override
    public AutoscaleClusterHistoryResponse convert(History source) {
        AutoscaleClusterHistoryResponse json = new AutoscaleClusterHistoryResponse();
        json.setId(source.getId());
        json.setAdjustment(source.getAdjustment());
        json.setAdjustmentType(source.getAdjustmentType());
        json.setAlertType(source.getAlertType());
        json.setStackCrn(source.getStackCrn());
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
