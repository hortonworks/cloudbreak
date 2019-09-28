package com.sequenceiq.cloudbreak.api.endpoint.common;


import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;

public interface StackEndpoint {

    StackResponse getStackFromDefaultWorkspace(@PathParam("name") String name, @QueryParam("entry") Set<String> entries);

    StackResponse get(@PathParam("id") Long id, @QueryParam("entry") Set<String> entries);

    void deleteInDefaultWorkspace(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    void deleteById(@PathParam("id") Long id, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    Map<String, Object> status(@PathParam("id") Long id);

    PlatformVariantsJson variants();

    default Response deleteInstance(@PathParam("stackId") Long stackId, @PathParam("instanceId") String instanceId) {
        return deleteInstance(stackId, instanceId, false);
    }

    Response deleteInstance(@PathParam("stackId") Long stackId, @PathParam("instanceId") String instanceId, @QueryParam("forced") boolean forced);

    Response deleteInstances(@PathParam("stackId") Long stackId, @QueryParam("instanceIds") Set<String> instanceIds);

    Response validate(@Valid StackValidationRequest stackValidationRequest);
}
