package com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Path("/v4/cloudprovider")
@Consumes(MediaType.APPLICATION_JSON)
public interface CloudProviderServicesV4Endopint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    ObjectStorageMetadataResponse getObjectStorageMetaData(@Valid ObjectStorageMetadataRequest request);
}
