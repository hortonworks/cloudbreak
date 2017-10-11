package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.LdapValidationRequest;
import com.sequenceiq.cloudbreak.api.model.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.RdsTestResult;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/util")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/util", description = ControllerDescription.UTIL_DESCRIPTION, protocols = "http,https")
public interface UtilEndpoint {

    @POST
    @Path("rds")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_RDS_CONNECTION, produces = ContentType.JSON, nickname = "testRdsConnectionUtil")
    RdsTestResult testRdsConnection(@Valid RDSConfigRequest rdsConfigRequest);

    @GET
    @Path("rds/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_RDS_CONNECTION_BY_ID, produces = ContentType.JSON,
            nickname = "testRdsConnectionByIdUtil")
    RdsTestResult testRdsConnectionById(@PathParam("id") Long id);

    @POST
    @Path("ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_LDAP_CONNECTION, produces = ContentType.JSON, nickname = "testLdapConnectionUtil")
    LdapTestResult testLdapConnection(@Valid LdapValidationRequest ldapValidationRequest);

    @GET
    @Path("ldap/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_LDAP_CONNECTION_BY_ID, produces = ContentType.JSON,
            nickname = "testLdapConnectionByIdUtil")
    LdapTestResult testLdapConnectionById(@PathParam("id") Long id);

    @POST
    @Path("ambari-database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.TEST_DATABASE, produces = ContentType.JSON, nickname = "testAmbariDatabaseUtil")
    AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson);

    @POST
    @Path("rds-database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CREATE_DATABASE, produces = ContentType.JSON, nickname = "createRDSDatabaseUtil")
    RdsBuildResult buildRdsConnection(@Valid RDSBuildRequest rdsBuildRequest, @QueryParam("target") Set<String> targets);

}
