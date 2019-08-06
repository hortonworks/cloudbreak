package com.sequenceiq.freeipa.api.v1.kerberosmgmt;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelNotes;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabOperationsDescription;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/kerberosmgmt")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/kerberosmgmt", protocols = "http,https")
public interface KerberosMgmtV1Endpoint {

    @POST
    @Path("servicekeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KeytabOperationsDescription.DESCRIBE_GENERATE_SERVICE_KEYTAB, produces = ContentType.JSON,
            notes = KeytabModelNotes.GENERATE_KEYTAB_NOTES,
            nickname = "generateServiceKeytabV1")
    ServiceKeytabResponse generateServiceKeytab(@Valid ServiceKeytabRequest request) throws Exception;

    @GET
    @Path("servicekeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KeytabOperationsDescription.DESCRIBE_SERVICE_KEYTAB, produces = ContentType.JSON, notes = KeytabModelNotes.GET_KEYTAB_NOTES,
            nickname = "getServiceKeytabV1")
    ServiceKeytabResponse getServiceKeytab(@Valid ServiceKeytabRequest request) throws Exception;

    @DELETE
    @Path("serviceprincipal")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KeytabOperationsDescription.DESCRIBE_DELETE_SERVICE_PRINCIPAL, produces = ContentType.JSON,
            notes = KeytabModelNotes.DELETE_SERVICE_PRINCIPAL_NOTES,
            nickname = "deleteServicePrinciapalV1")
    void deleteServicePrincipal(@Valid ServicePrincipalRequest request) throws Exception;

    @DELETE
    @Path("host")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KeytabOperationsDescription.DESCRIBE_DELETE_HOST, produces = ContentType.JSON, notes = KeytabModelNotes.DELETE_HOST_NOTES,
            nickname = "deleteHostV1")
    void deleteHost(@Valid HostRequest request) throws Exception;

    @DELETE
    @Path("cleanupClusterSecrets")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KeytabOperationsDescription.DESCRIBE_CLEANUP, produces = ContentType.JSON, notes = KeytabModelNotes.CLEANUP_NOTES,
            nickname = "cleanupClusterSecretsV1")
    void cleanupClusterSecrets(@Valid VaultCleanupRequest request) throws Exception;
}