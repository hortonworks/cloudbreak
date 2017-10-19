package com.sequenceiq.cloudbreak.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/imagecatalogs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_DESCRIPTION, protocols = "http,https")
public interface ImageCatalogEndpoint {

    @GET
    @Path("{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_NOTES, nickname = "getImagesByProvider")
    ImagesResponse getImagesByProvider(@PathParam("platform") String platform) throws Exception;
}
