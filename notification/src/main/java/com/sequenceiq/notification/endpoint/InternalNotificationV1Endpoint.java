package com.sequenceiq.notification.endpoint;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.test.TestOnlyInternalRegisterAzureOutboundNotificationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/internal/test_notification")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/internal/test_notification")
public interface InternalNotificationV1Endpoint {

    @POST
    @Path("send_weekly")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "send notifications",
            operationId = "sendNotifications",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void testSendWeeklyNotification(@Valid TestOnlyInternalRegisterAzureOutboundNotificationRequest testOnly);

    @POST
    @Path("register_azure_default_outbound")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "register azure default outbound notifications",
            operationId = "registerAzureDefaultOutbound",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void testRegisterAzureDefaultOutbound(@Valid TestOnlyInternalRegisterAzureOutboundNotificationRequest request);

    @POST
    @Path("create_distribution_list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "creates distribution list",
            operationId = "createDistributionList",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void testCreateOrUpdateDistributionLists(@QueryParam("resourceCrn") String resourceCrn);

    @DELETE
    @Path("delete_distribution_list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "deletes distribution list",
            operationId = "deleteDistributionList",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void testDeleteDistributionLists(@QueryParam("resourceCrn") String resourceCrn);

    @GET
    @Path("list_distribution_list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "list distribution list",
            operationId = "listDistributionList",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DistributionList> testListDistributionLists(@QueryParam("resourceCrn") String resourceCrn);
}
