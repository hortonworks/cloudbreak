package com.sequenceiq.cloudbreak.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RdsTestResult;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/util")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/util", description = ControllerDescription.UTIL_DESCRIPTION, protocols = "http,https")
public interface UtilEndpoint {

    @POST
    @Path("rds")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.TEST_RDS_CONNECTION, produces = ContentType.JSON, nickname = "testRdsConnectionUtil")
    RdsTestResult testRdsConnection(@Valid RDSConfigJson rdsConfigJson);

    @POST
    @Path("ldap")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.TEST_LDAP_CONNECTION, produces = ContentType.JSON, nickname = "testLdapConnectionUtil")
    LdapTestResult testLdapConnection(@Valid LdapConfigRequest ldapConfigRequest);

    @POST
    @Path("ambari-database")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.TEST_DATABASE, produces = ContentType.JSON, nickname = "testAmbariDatabaseUtil")
    AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson);

}
