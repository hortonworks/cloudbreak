package com.sequenceiq.cloudbreak.api.endpoint.v1;

import static com.sequenceiq.cloudbreak.doc.Notes.IMAGE_CATALOG_NOTES;

import java.util.List;

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

@Path("/v1/imagecatalogs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_DESCRIPTION, protocols = "http,https")
public interface ImageCatalogV1Endpoint {

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_PUBLICS_IMAGE_CATALOGS, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getPublicsImageCatalogs")
    List<ImageCatalogResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_PUBLIC_IMAGE_CATALOG_BY_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getPublicImageCatalogsByName")
    ImageCatalogResponse getByName(@PathParam("name") String name, @QueryParam("withImages") boolean withImages);

    @GET
    @Path("account/{name}/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getPublicImagesByProviderAndCustomImageCatalog")
    ImagesResponse getImagesByProviderFromImageCatalog(@PathParam("name") String name, @PathParam("platform") String platform) throws Exception;

    @GET
    @Path("{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_PROVIDER, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImagesByProvider")
    ImagesResponse getImagesByProvider(@PathParam("platform") String platform) throws Exception;

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "postPublicImageCatalog")
    ImageCatalogResponse postPublic(@Valid ImageCatalogRequest imageCatalogRequest);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "postPrivateImageCatalog")
    ImageCatalogResponse postPrivate(@Valid ImageCatalogRequest imageCatalogRequest);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "deletePublicImageCatalogByName")
    void deletePublic(@PathParam("name") String name);

    @PUT
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putPublicImageCatalog")
    ImageCatalogResponse putPublic(@Valid UpdateImageCatalogRequest request);

    @PUT
    @Path("setdefault/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.PUT_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "putSetDefaultImageCatalogByName")
    ImageCatalogResponse putSetDefaultByName(@PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_BY_IMAGE_CATALOG_NAME, produces = ContentType.JSON,
            notes = IMAGE_CATALOG_NOTES, nickname = "getImageCatalogRequestFromName")
    ImageCatalogRequest getRequestfromName(@PathParam("name") String name);

    @GET
    @Path("account/upgrade/{stackName}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME_AND_CUSTOM_IMAGE_CATALOG, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndCustomImageCatalog")
    ImagesResponse getImagesFromCustomImageCatalogByStack(@PathParam("name") String name, @PathParam("stackName") String stackName) throws Exception;

    @GET
    @Path("account/upgrade/{stackName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ImageCatalogOpDescription.GET_IMAGES_BY_STACK_NAME, produces = ContentType.JSON,
            notes = Notes.IMAGE_CATALOG_STACK_UPGRADE_NOTES, nickname = "getImagesByStackNameAndDefaultImageCatalog")
    ImagesResponse getImagesFromDefaultImageCatalogByStack(@PathParam("stackName") String stackName) throws Exception;

}
