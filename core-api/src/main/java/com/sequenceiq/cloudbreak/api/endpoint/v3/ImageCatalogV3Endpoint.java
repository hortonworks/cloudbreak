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
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "getImageCatalogInOrganization")
    ImageCatalogResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @QueryParam("withImages") boolean withImages);

    @GET
    @Path("{name}/platform/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImagesByProviderAndCustomImageCatalogInOrganization")
    ImagesResponse getImagesByProviderFromImageCatalogInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @PathParam("platform") String platform) throws Exception;

    @GET
    @Path("platform/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "getImagesByProvider")
    ImagesResponse getImagesByProvider(@PathParam("organizationId") Long organizationId, @PathParam("platform") String platform) throws Exception;

    @GET
    @Path("upgrade/{stackName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndDefaultImageCatalogInOrganization")
    ImagesResponse getImagesFromDefaultImageCatalogByStackInOrganization(@PathParam("organizationId") Long organizationId,
            @PathParam("stackName") String stackName) throws Exception;

    @GET
    @Path("upgrade/{stackName}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndCustomImageCatalogInOrganization")
    ImagesResponse getImagesFromCustomImageCatalogByStackInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @PathParam("stackName") String stackName) throws Exception;

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "createImageCatalogInOrganization")
    ImageCatalogResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid ImageCatalogRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.IMAGE_CATALOG_NOTES,
            nickname = "deleteImageCatalogInOrganization")
    ImageCatalogResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putPublicImageCatalogInOrganization")
    ImageCatalogResponse putPublicInOrganization(@PathParam("organizationId") Long organizationId, @Valid UpdateImageCatalogRequest request);

    @PUT
    @Path("setdefault/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putSetDefaultImageCatalogByNameInOrganization")
    ImageCatalogResponse putSetDefaultByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_IMAGE_CATALOG_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImageCatalogRequestFromName")
    ImageCatalogRequest getRequestFromName(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
