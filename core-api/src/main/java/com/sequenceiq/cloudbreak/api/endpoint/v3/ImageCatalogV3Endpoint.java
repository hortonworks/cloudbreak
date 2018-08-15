package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ImageCatalogOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/imagecatalogs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_V3_DESCRIPTION, protocols = "http,https")
public interface ImageCatalogV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "listImageCatalogsByOrganization")
    Set<ImageCatalogResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "getImageCatalogInOrganization")
    ImageCatalogResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "createImageCatalogInOrganization")
    ImageCatalogResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid ImageCatalogRequest request);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "deleteImageCatalogInOrganization")
    ImageCatalogResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
