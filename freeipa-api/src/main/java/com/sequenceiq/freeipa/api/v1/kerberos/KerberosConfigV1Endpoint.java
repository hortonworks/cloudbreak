package com.sequenceiq.freeipa.api.v1.kerberos;

import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigNotes.KERBEROS_CONFIG_NOTES;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.CREATE_FOR_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.DELETE_BY_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.DESCRIBE_FOR_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.GET_REQUEST;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.KERBEROS_CONFIG_V4_DESCRIPTION;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/kerberos")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/kerberos", description = KERBEROS_CONFIG_V4_DESCRIPTION, protocols = "http,https")
public interface KerberosConfigV1Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DESCRIBE_FOR_ENVIRONMENT, produces = ContentType.JSON, notes = KERBEROS_CONFIG_NOTES, nickname = "getKerberosConfigForEnvironment")
    DescribeKerberosConfigResponse describe(@QueryParam("environmentId") @NotEmpty String environmentId);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CREATE_FOR_ENVIRONMENT, produces = ContentType.JSON, notes = KERBEROS_CONFIG_NOTES, nickname = "createKerberosConfigForEnvironment")
    DescribeKerberosConfigResponse create(@Valid CreateKerberosConfigRequest request);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_BY_ENVIRONMENT, produces = ContentType.JSON, notes = KERBEROS_CONFIG_NOTES, nickname = "deleteKerberosConfigForEnvironment")
    void delete(@QueryParam("environmentId") @NotEmpty String environmentId);

    @GET
    @Path("request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_REQUEST, produces = ContentType.JSON, notes = KERBEROS_CONFIG_NOTES, nickname = "getCreateKerberosRequestForEnvironment")
    CreateKerberosConfigRequest getRequest(@QueryParam("environmentId") @NotEmpty String environmentId);
}
