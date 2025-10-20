package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.doc.Notes.IMAGE_CATALOG_NOTES;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageRecommendationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.RuntimeVersionsV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ImageCatalogOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/image_catalogs")
@Tag(name = "/v4/{workspaceId}/imagecatalogs", description = ControllerDescription.IMAGE_CATALOG_V4_DESCRIPTION)
public interface ImageCatalogV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.LIST_BY_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "listImageCatalogsByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("custom") boolean customCatalogsOnly);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_BY_NAME_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "getImageCatalogInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("withImages") @DefaultValue("false") Boolean withImages,
            @QueryParam("applyVersionBasedFiltering") @DefaultValue("true") Boolean applyVersionBasedFiltering);

    @GET
    @Path("name/{name}/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_BY_NAME_IN_WORKSPACE_INTERNAL, description = IMAGE_CATALOG_NOTES,
            operationId = "getImageCatalogInWorkspaceInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response getByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("withImages") @DefaultValue("false") Boolean withImages,
            @QueryParam("applyVersionBasedFiltering") @DefaultValue("true") Boolean applyVersionBasedFiltering,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_BY_CRN_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "getImageCatalogByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("withImages") @DefaultValue("false") Boolean withImages);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.CREATE_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "createImageCatalogInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid ImageCatalogV4Request request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.DELETE_BY_NAME_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "deleteImageCatalogInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.DELETE_BY_CRN_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "deleteImageCatalogByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, description = IMAGE_CATALOG_NOTES,
            operationId = "deleteImageCatalogsInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.PUT_BY_NAME, description = IMAGE_CATALOG_NOTES,
            operationId = "updateImageCatalogInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Response update(@PathParam("workspaceId") Long workspaceId, @Valid UpdateImageCatalogV4Request request);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_BY_IMAGE_CATALOG_NAME,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageCatalogRequestFromNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageCatalogV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGES,
            description = IMAGE_CATALOG_NOTES, operationId = "getImagesInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @SuppressWarnings("checkstyle:ParameterNumber")
    ImagesV4Response getImages(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("stackName") String stackName,
            @QueryParam("platform") String platform,
            @QueryParam("runtimeVersion") String runtimeVersion,
            @QueryParam("imageType") String imageType,
            @QueryParam("govCloud") boolean govCloud,
            @QueryParam("defaultOnly") boolean defaultOnly,
            @QueryParam("architecture") String architecture) throws Exception;

    @GET
    @Path("{name}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGES_BY_NAME,
            description = IMAGE_CATALOG_NOTES, operationId = "getImagesByNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @SuppressWarnings("checkstyle:ParameterNumber")
    ImagesV4Response getImagesByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("stackName") String stackName,
            @QueryParam("platform") String platform,
            @QueryParam("runtimeVersion") String runtimeVersion,
            @QueryParam("imageType") String imageType,
            @QueryParam("govCloud") boolean govCloud,
            @QueryParam("defaultOnly") boolean defaultOnly,
            @QueryParam("architecture") String architecture) throws Exception;

    @GET
    @Path("image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_BY_ID,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageById",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImagesV4Response getImageByImageId(@PathParam("workspaceId") Long workspaceId, @QueryParam("imageId") String imageId,
            @QueryParam("accountId") String accountId) throws Exception;

    @GET
    @Path("{name}/image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_BY_NAME_AND_ID,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageByNameAndId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImagesV4Response getImageByCatalogNameAndImageId(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("imageId") String imageId, @QueryParam("accountId") String accountId) throws Exception;

    @GET
    @Path("{name}/singleimage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_BY_NAME_AND_ID,
            description = IMAGE_CATALOG_NOTES, operationId = "getSingleImageByNameAndId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageV4Response getSingleImageByCatalogNameAndImageId(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("imageId") String imageId) throws Exception;

    @GET
    @Path("{name}/singleimage/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_BY_NAME_AND_ID, description = IMAGE_CATALOG_NOTES,
            operationId = "getSingleImageByNameAndIdInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageV4Response getSingleImageByCatalogNameAndImageIdInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("imageId") String imageId, @QueryParam("accountId") String accountId) throws Exception;

    @GET
    @Path("image/{imageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_FROM_DEFAULT_BY_ID,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageFromDefaultById",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageV4Response getImageFromDefaultById(@PathParam("workspaceId") Long workspaceId,
            @PathParam("imageId") String imageId) throws Exception;

    @GET
    @Path("image/type/{type}/provider/{provider}/runtime/{runtime}/govCloud/{govCloud}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_FROM_DEFAULT,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageFromDefaultWithRuntime",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageV4Response getImageFromDefault(@PathParam("workspaceId") Long workspaceId,
            @PathParam("type") String type,
            @PathParam("provider") String provider,
            @PathParam("runtime") String runtime,
            @PathParam("govCloud") boolean govCloud,
            @QueryParam("architecture") String architecture) throws Exception;

    @GET
    @Path("image/type/{type}/provider/{provider}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_IMAGE_FROM_DEFAULT,
            description = IMAGE_CATALOG_NOTES, operationId = "getImageFromDefault",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageV4Response getImageFromDefault(@PathParam("workspaceId") Long workspaceId,
            @PathParam("type") String type,
            @PathParam("provider") String provider) throws Exception;

    @GET
    @Path("default_runtime_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.GET_DEFAULT_IMAGE_CATALOG_RUNTIME_VERSIONS,
            description = IMAGE_CATALOG_NOTES, operationId = "getRuntimeVersionsFromDefault",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RuntimeVersionsV4Response getRuntimeVersionsFromDefault(@PathParam("workspaceId") Long workspaceId) throws Exception;

    @POST
    @Path("validate_recommended")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ImageCatalogOpDescription.VALIDATE_RECOMMENDED_IMAGE_WITH_PROVIDER, description = IMAGE_CATALOG_NOTES,
            operationId = "validateRecommendedImageWithProvider",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ImageRecommendationV4Response validateRecommendedImageWithProvider(@PathParam("workspaceId") Long workspaceId, @Valid ImageRecommendationV4Request request)
            throws Exception;
}
