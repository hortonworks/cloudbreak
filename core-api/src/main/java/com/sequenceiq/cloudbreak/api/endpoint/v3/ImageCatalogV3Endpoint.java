package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.Notes.IMAGE_CATALOG_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.UpdateImageCatalogRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ImageCatalogOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/imagecatalogs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_V3_DESCRIPTION, protocols = "http,https")
public interface ImageCatalogV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "listImageCatalogsByWorkspace")
    Set<ImageCatalogResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "getImageCatalogInWorkspace")
    ImageCatalogResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("withImages") boolean withImages);

    @GET
    @Path("{name}/platform/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImagesByProviderAndCustomImageCatalogInWorkspace")
    ImagesResponse getImagesByProviderFromImageCatalogInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @PathParam("platform") String platform) throws Exception;

    @GET
    @Path("platform/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "getImagesByProviderInWorkspace")
    ImagesResponse getImagesByProvider(@PathParam("workspaceId") Long workspaceId, @PathParam("platform") String platform) throws Exception;

    @GET
    @Path("upgrade/{stackName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndDefaultImageCatalogInWorkspace")
    ImagesResponse getImagesFromDefaultImageCatalogByStackInWorkspace(@PathParam("workspaceId") Long workspaceId,
            @PathParam("stackName") String stackName) throws Exception;

    @GET
    @Path("upgrade/{stackName}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndCustomImageCatalogInWorkspace")
    ImagesResponse getImagesFromCustomImageCatalogByStackInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @PathParam("stackName") String stackName) throws Exception;

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "createImageCatalogInWorkspace")
    ImageCatalogResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid ImageCatalogRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "deleteImageCatalogInWorkspace")
    ImageCatalogResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putPublicImageCatalogInWorkspace")
    ImageCatalogResponse putPublicInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid UpdateImageCatalogRequest request);

    @PUT
    @Path("setdefault/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putSetDefaultImageCatalogByNameInWorkspace")
    ImageCatalogResponse putSetDefaultByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_IMAGE_CATALOG_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImageCatalogRequestFromNameInWorkspace")
    ImageCatalogRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
