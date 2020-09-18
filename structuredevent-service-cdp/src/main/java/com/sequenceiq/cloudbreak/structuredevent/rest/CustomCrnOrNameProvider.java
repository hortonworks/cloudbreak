package com.sequenceiq.cloudbreak.structuredevent.rest;

import java.util.Map;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;

public interface CustomCrnOrNameProvider {

    Map<String, String> provide(RestCallDetails restCallDetails, CDPOperationDetails operationDetails, Map<String, String> restParams,
            String nameField, String crnField);
}
