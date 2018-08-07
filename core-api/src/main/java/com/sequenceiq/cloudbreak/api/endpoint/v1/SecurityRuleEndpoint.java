package com.sequenceiq.cloudbreak.api.endpoint.v1;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SecurityRuleOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/securityrules")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/securityrules", description = ControllerDescription.SECURITY_RULE_DESCRIPTION, protocols = "http,https")
public interface SecurityRuleEndpoint {

    @GET
    @Path("defaultsecurityrules")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityRuleOpDescription.GET_DEFAULT_SECURITY_RULES, produces = ContentType.JSON,
            notes = Notes.SECURITY_RULE_NOTES, nickname = "getDefaultSecurityRules")
    SecurityRulesResponse getDefaultSecurityRules();
}
