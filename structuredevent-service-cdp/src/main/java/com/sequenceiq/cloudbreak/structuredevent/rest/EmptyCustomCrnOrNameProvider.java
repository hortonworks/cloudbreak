package com.sequenceiq.cloudbreak.structuredevent.rest;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;

@Component
public class EmptyCustomCrnOrNameProvider implements CustomCrnOrNameProvider {
    @Override
    public Map<String, String> provide(RestCallDetails restCallDetails, CDPOperationDetails operationDetails, Map<String, String> restParams, String nameField,
            String crnField) {
        return null;
    }
}
