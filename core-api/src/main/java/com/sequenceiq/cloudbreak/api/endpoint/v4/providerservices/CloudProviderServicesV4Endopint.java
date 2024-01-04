package com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

@RetryAndMetrics
@Path("/v4/cloudprovider")
@Consumes(MediaType.APPLICATION_JSON)
public interface CloudProviderServicesV4Endopint {

    @POST
    @Path("objectstorage/getmetadata")
    @Produces(MediaType.APPLICATION_JSON)
    ObjectStorageMetadataResponse getObjectStorageMetaData(@Valid ObjectStorageMetadataRequest request);

    @POST
    @Path("objectstorage/validate")
    @Produces(MediaType.APPLICATION_JSON)
    ObjectStorageValidateResponse validateObjectStorage(@Valid ObjectStorageValidateRequest request);

    @POST
    @Path("nosql/getmetadata")
    @Produces(MediaType.APPLICATION_JSON)
    NoSqlTableMetadataResponse getNoSqlTableMetaData(@Valid NoSqlTableMetadataRequest request);

    @DELETE
    @Path("nosql/table")
    @Produces(MediaType.APPLICATION_JSON)
    NoSqlTableDeleteResponse deleteNoSqlTable(@Valid NoSqlTableDeleteRequest request);

}
