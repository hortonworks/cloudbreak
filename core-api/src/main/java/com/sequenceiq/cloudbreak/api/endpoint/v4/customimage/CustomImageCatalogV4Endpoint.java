package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4CreateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4UpdateImageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4CreateResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4DeleteResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetImageResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4GetResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4ListResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response.CustomImageCatalogV4UpdateImageResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static com.sequenceiq.cloudbreak.doc.ControllerDescription.CUSTOM_IMAGE_CATALOG_V4_DESCRIPTION;
import static com.sequenceiq.cloudbreak.doc.Notes.CUSTOM_IMAGE_CATALOG_NOTES;
import static com.sequenceiq.cloudbreak.doc.Notes.IMAGE_CATALOG_NOTES;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.CREATE_IN_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.CREATE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.DELETE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.DELETE_FROM_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.GET_BY_NAME_IN_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.GET_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.LIST;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.CustomImageCatalogOpDescription.UPDATE_IN_CATALOG;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RetryAndMetrics
@Consumes(APPLICATION_JSON)
@Path("/v4/custom_image_catalogs")
@Api(value = "/v4/customimagecatalogs", description = CUSTOM_IMAGE_CATALOG_V4_DESCRIPTION, protocols = "http,https",
        consumes = APPLICATION_JSON)
public interface CustomImageCatalogV4Endpoint {

    @GET
    @Path("")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = LIST, produces = APPLICATION_JSON, notes = CUSTOM_IMAGE_CATALOG_NOTES,
            nickname = "listCustomImageCatalogs")
    CustomImageCatalogV4ListResponse list();

    @GET
    @Path("name/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = GET_BY_NAME, produces = APPLICATION_JSON, notes = CUSTOM_IMAGE_CATALOG_NOTES,
            nickname = "getCustomImageCatalogByName")
    CustomImageCatalogV4GetResponse get(@PathParam("name") String name);

    @POST
    @Path("")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = CREATE, produces = APPLICATION_JSON, notes = CUSTOM_IMAGE_CATALOG_NOTES,
            nickname = "createCustomImageCatalog")
    CustomImageCatalogV4CreateResponse create(@Valid CustomImageCatalogV4CreateRequest request);

    @DELETE
    @Path("name/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = DELETE_BY_NAME, produces = APPLICATION_JSON, notes = CUSTOM_IMAGE_CATALOG_NOTES,
            nickname = "deleteCustomImageCatalogByName")
    CustomImageCatalogV4DeleteResponse delete(@PathParam("name") String name);

    @GET
    @Path("{name}/image/{imageId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = GET_BY_NAME_IN_CATALOG, produces = APPLICATION_JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "getCustomImage")
    CustomImageCatalogV4GetImageResponse getCustomImage(@PathParam("name") String name, @PathParam("imageId") String imageId);

    @POST
    @Path("{name}/image")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = CREATE_IN_CATALOG, produces = APPLICATION_JSON, notes = CUSTOM_IMAGE_CATALOG_NOTES,
            nickname = "createCustomImage")
    CustomImageCatalogV4CreateImageResponse createCustomImage(@PathParam("name") String name,
            @Valid CustomImageCatalogV4CreateImageRequest request);

    @PUT
    @Path("{name}/image/{imageId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = UPDATE_IN_CATALOG, produces = APPLICATION_JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "updateCustomImage")
    CustomImageCatalogV4UpdateImageResponse updateCustomImage(@PathParam("name") String name, @PathParam("imageId") String imageId,
            @Valid CustomImageCatalogV4UpdateImageRequest request);

    @DELETE
    @Path("{name}/image/{imageId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = DELETE_FROM_CATALOG, produces = APPLICATION_JSON, notes = IMAGE_CATALOG_NOTES,
            nickname = "deleteCustomImage")
    CustomImageCatalogV4DeleteImageResponse deleteCustomImage(@PathParam("name") String name, @PathParam("imageId") String imageId);

}
