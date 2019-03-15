package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.doc.Notes.IMAGE_CATALOG_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ImageCatalogOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/image_catalogs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_V4_DESCRIPTION, protocols = "http,https")
public interface ImageCatalogV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "listImageCatalogsByWorkspace")
    ImageCatalogV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "getImageCatalogInWorkspace")
    ImageCatalogV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("withImages") @DefaultValue("false") Boolean withImages);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "createImageCatalogInWorkspace")
    ImageCatalogV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid ImageCatalogV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "deleteImageCatalogInWorkspace")
    ImageCatalogV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "deleteImageCatalogsInWorkspace")
    ImageCatalogV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "updateImageCatalogInWorkspace")
    ImageCatalogV4Response update(@PathParam("workspaceId") Long workspaceId, @Valid UpdateImageCatalogV4Request request);

    @PUT
    @Path("{name}/set_default")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "setDefaultImageCatalogByNameInWorkspace")
    ImageCatalogV4Response setDefault(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_IMAGE_CATALOG_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImageCatalogRequestFromNameInWorkspace")
    ImageCatalogV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImagesInWorkspace")
    ImagesV4Response getImages(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("stackName") String stackName, @QueryParam("platform") String platform) throws Exception;

    @GET
    @Path("{name}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImagesByNameInWorkspace")
    ImagesV4Response getImagesByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("stackName") String stackName, @QueryParam("platform") String platform) throws Exception;

}
