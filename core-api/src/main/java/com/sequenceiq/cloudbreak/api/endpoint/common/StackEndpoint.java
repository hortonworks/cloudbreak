package com.sequenceiq.cloudbreak.api.endpoint.common;


import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest;

public interface StackEndpoint {

    Set<StackResponse> getPrivates();

    Set<StackResponse> getPublics();

    StackResponse getPrivate(@PathParam("name") String name, @QueryParam("entry") Set<String> entries);

    StackResponse getPublic(@PathParam("name") String name, @QueryParam("entry") Set<String> entries);

    StackResponse get(@PathParam("id") Long id, @QueryParam("entry") Set<String> entries);

    void deletePublic(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    void deletePrivate(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    void delete(@PathParam("id") Long id, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    Map<String, Object> status(@PathParam("id") Long id);

    PlatformVariantsJson variants();

    Response deleteInstance(@PathParam("stackId") Long stackId, @PathParam("instanceId") String instanceId);

    CertificateResponse getCertificate(@PathParam("id") Long stackId);

    Response validate(@Valid StackValidationRequest stackValidationRequest);

    StackResponse getStackForAmbari(@Valid AmbariAddressJson json);

    Set<AutoscaleStackResponse> getAllForAutoscale();
}
