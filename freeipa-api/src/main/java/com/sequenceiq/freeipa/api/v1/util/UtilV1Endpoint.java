package com.sequenceiq.freeipa.api.v1.util;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.util.doc.UtilDescriptions;
import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/utils", description = UtilDescriptions.NOTES, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface UtilV1Endpoint {

    @GET
    @Path("used_images")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilDescriptions.USED_IMAGES, produces = MediaType.APPLICATION_JSON, nickname = "usedImagesV1")
    UsedImagesListV1Response usedImages();
}

