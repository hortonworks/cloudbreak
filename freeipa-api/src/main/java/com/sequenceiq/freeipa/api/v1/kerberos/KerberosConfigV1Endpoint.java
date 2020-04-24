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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/kerberos")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/kerberos", description = KERBEROS_CONFIG_V4_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface KerberosConfigV1Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DESCRIBE_FOR_ENVIRONMENT, produces = MediaType.APPLICATION_JSON, notes = KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosConfigForEnvironment")
    DescribeKerberosConfigResponse describe(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosConfigOperationDescription.GET_BY_ENV_FOR_CLUSTER, produces = MediaType.APPLICATION_JSON, notes = KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosConfigForClusterV1")
    DescribeKerberosConfigResponse getForCluster(@QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("clusterName") @NotEmpty String clusterName) throws Exception;

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CREATE_FOR_ENVIRONMENT, produces = MediaType.APPLICATION_JSON, notes = KERBEROS_CONFIG_NOTES,
            nickname = "createKerberosConfigForEnvironment")
    DescribeKerberosConfigResponse create(@Valid CreateKerberosConfigRequest request);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_BY_ENVIRONMENT, produces = MediaType.APPLICATION_JSON, notes = KERBEROS_CONFIG_NOTES,
            nickname = "deleteKerberosConfigForEnvironment")
    void delete(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_REQUEST, produces = MediaType.APPLICATION_JSON, notes = KERBEROS_CONFIG_NOTES,
            nickname = "getCreateKerberosRequestForEnvironment")
    CreateKerberosConfigRequest getRequest(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
