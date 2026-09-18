package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterOutcome;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

class MaintenanceTaskSubmitterHttpClientTest {

    @Test
    void dispatchReturnsFailedWhenSubmitterBaseUrlMissing() {
        MaintenanceTaskSubmitterHttpClient client = client(new SubmitterServiceEndpointResolver(Map.of()), mock(Client.class));
        MaintenanceWindowTask task = httpExecuteTask();

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                task, run(task), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.FAILED);
        assertThat(result.errorDetail()).contains("No submitter base URL");
    }

    @Test
    void dispatchReturnsFailedWhenExecutePathMissing() {
        MaintenanceTaskSubmitterHttpClient client = client(
                new SubmitterServiceEndpointResolver(Map.of("datalake", "http://datalake:8080/dl")),
                mock(Client.class));
        MaintenanceWindowTask task = httpExecuteTask();
        task.setExecutionRef(new Json(Map.of("submitter_service", "datalake")));

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                task, run(task), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.FAILED);
        assertThat(result.errorDetail()).contains("execute_path");
    }

    @Test
    void dispatchReturnsSyncCompletedOnHttp200() {
        MaintenanceTaskSubmitterHttpClient client = clientWithResponse(Response.ok().build());

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                httpExecuteTask(), run(httpExecuteTask()), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.SYNC_COMPLETED);
        assertThat(result.errorDetail()).isNull();
    }

    @Test
    void dispatchReturnsAsyncAcceptedOnHttp202() {
        MaintenanceTaskSubmitterHttpClient client = clientWithResponse(Response.status(Response.Status.ACCEPTED).build());

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                httpExecuteTask(), run(httpExecuteTask()), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.ASYNC_ACCEPTED);
    }

    @Test
    void dispatchReturnsFailedOnHttp500() {
        MaintenanceTaskSubmitterHttpClient client = clientWithResponse(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("upstream failed").build());

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                httpExecuteTask(), run(httpExecuteTask()), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.FAILED);
        assertThat(result.errorDetail()).contains("HTTP 500");
    }

    @Test
    void dispatchReturnsFailedOnHttp400() {
        MaintenanceTaskSubmitterHttpClient client = clientWithResponse(
                Response.status(Response.Status.BAD_REQUEST).entity("bad payload").build());

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                httpExecuteTask(), run(httpExecuteTask()), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.FAILED);
        assertThat(result.errorDetail()).contains("HTTP 400");
    }

    @Test
    void dispatchReturnsFailedWithErrorDetailOnProcessingException() {
        SubmitterServiceEndpointResolver resolver = new SubmitterServiceEndpointResolver(
                Map.of("datalake", "http://datalake:8080/dl"));
        Client restClient = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        MaintenanceSubmitterOutboundRequestBuilder outbound = mock(MaintenanceSubmitterOutboundRequestBuilder.class);
        when(restClient.target("http://datalake:8080/dl/internal/maintenance-tasks/execute")).thenReturn(webTarget);
        when(webTarget.property(anyString(), any())).thenReturn(webTarget);
        when(outbound.prepareJsonPost(webTarget)).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenThrow(new ProcessingException("connection reset"));
        MaintenanceTaskSubmitterHttpClient client = client(resolver, restClient, outbound);

        MaintenanceTaskSubmitterDispatchResult result = client.invokeExecuteCallback(
                httpExecuteTask(), run(httpExecuteTask()), schedule(), occurrence(), "42:v1");

        assertThat(result.outcome()).isEqualTo(MaintenanceTaskSubmitterOutcome.FAILED);
        assertThat(result.errorDetail()).contains("connection reset");
    }

    @Test
    void joinUrlAddsSlashWhenExecutePathOmitsLeadingSlash() {
        assertThat(MaintenanceTaskSubmitterHttpClient.joinUrl("http://datalake:8080/dl", "internal/foo"))
                .isEqualTo("http://datalake:8080/dl/internal/foo");
    }

    @Test
    void dispatchIdempotencyKeyUsesRunIdAndAttemptCount() {
        MaintenanceWindowRun run = run(httpExecuteTask());
        run.setId(42L);
        run.setAttemptCount(3);

        assertThat(MaintenanceTaskSubmitterHttpClient.dispatchIdempotencyKey(run)).isEqualTo("42:3");
    }

    @Test
    void dispatchIncludesWindowBoundsAndPayloadInRequestBody() {
        SubmitterServiceEndpointResolver resolver = new SubmitterServiceEndpointResolver(
                Map.of("datalake", "http://datalake:8080/dl"));
        Client restClient = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        MaintenanceSubmitterOutboundRequestBuilder outbound = mock(MaintenanceSubmitterOutboundRequestBuilder.class);
        when(restClient.target("http://datalake:8080/dl/internal/maintenance-tasks/execute")).thenReturn(webTarget);
        when(webTarget.property(anyString(), any())).thenReturn(webTarget);
        when(outbound.prepareJsonPost(webTarget)).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(Response.ok().build());
        MaintenanceTaskSubmitterHttpClient client = client(resolver, restClient, outbound);

        MaintenanceWindowTask task = httpExecuteTask();
        task.setTaskPayload(new Json(Map.of("key", "value")));
        MaintenanceWindowRun run = run(task);
        run.setId(99L);
        run.setAttemptCount(2);
        client.invokeExecuteCallback(task, run, schedule(), occurrence(), "42:v1");

        ArgumentCaptor<Entity<?>> entityCaptor = ArgumentCaptor.forClass(Entity.class);
        verify(builder).header("Idempotency-Key", "99:2");
        verify(builder).post(entityCaptor.capture());
        MaintenanceTaskDispatchRequest body = (MaintenanceTaskDispatchRequest) entityCaptor.getValue().getEntity();
        assertThat(body.idempotencyKey()).isEqualTo("99:2");
        assertThat(body.taskId()).isEqualTo(7L);
        assertThat(body.runId()).isEqualTo(99L);
        assertThat(body.windowStart()).isEqualTo(1L);
        assertThat(body.windowEnd()).isEqualTo(2L);
        assertThat(body.taskPayload()).containsEntry("key", "value");
    }

    private MaintenanceTaskSubmitterHttpClient client(SubmitterServiceEndpointResolver resolver, Client restClient) {
        return client(resolver, restClient, passthroughOutboundBuilder());
    }

    private MaintenanceTaskSubmitterHttpClient client(
            SubmitterServiceEndpointResolver resolver,
            Client restClient,
            MaintenanceSubmitterOutboundRequestBuilder outbound) {
        RestClientFactory restClientFactory = mock(RestClientFactory.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(restClient);
        return new MaintenanceTaskSubmitterHttpClient(resolver, restClientFactory, outbound, 30_000, 120_000);
    }

    private MaintenanceTaskSubmitterHttpClient clientWithResponse(Response response) {
        SubmitterServiceEndpointResolver resolver = new SubmitterServiceEndpointResolver(
                Map.of("datalake", "http://datalake:8080/dl"));
        Client restClient = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(restClient.target("http://datalake:8080/dl/internal/maintenance-tasks/execute")).thenReturn(webTarget);
        when(webTarget.property(anyString(), any())).thenReturn(webTarget);
        MaintenanceSubmitterOutboundRequestBuilder outbound = mock(MaintenanceSubmitterOutboundRequestBuilder.class);
        when(outbound.prepareJsonPost(webTarget)).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);
        return client(resolver, restClient, outbound);
    }

    private static MaintenanceSubmitterOutboundRequestBuilder passthroughOutboundBuilder() {
        MaintenanceSubmitterOutboundRequestBuilder outbound = mock(MaintenanceSubmitterOutboundRequestBuilder.class);
        when(outbound.prepareJsonPost(any(WebTarget.class))).thenAnswer(invocation -> {
            WebTarget target = invocation.getArgument(0);
            return target.request(MediaType.APPLICATION_JSON);
        });
        return outbound;
    }

    private MaintenanceWindowTask httpExecuteTask() {
        MaintenanceWindowTask entity = new MaintenanceWindowTask();
        entity.setId(7L);
        entity.setAccountId("acc-1");
        entity.setResourceCrn("crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1");
        entity.setTaskType("TASK_A");
        entity.setWorkItemId("task-a");
        entity.setTaskKind(MaintenanceTaskKind.EVERY_WINDOW);
        entity.setStatus(MaintenanceTaskStatus.ACTIVE);
        entity.setSubmitterService("datalake");
        entity.setExecutionRef(new Json(Map.of(
                "submitter_service", "datalake",
                "execute_path", "/internal/maintenance-tasks/execute")));
        return entity;
    }

    private MaintenanceWindowRun run(MaintenanceWindowTask task) {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setId(99L);
        run.setMaintenanceWindowTask(task);
        run.setStatus(MaintenanceRunStatus.RUNNING);
        run.setAttemptCount(1);
        return run;
    }

    private MaintenanceWindowSchedule schedule() {
        MaintenanceWindowSchedule entity = new MaintenanceWindowSchedule();
        entity.setId(42L);
        entity.setVersion(1);
        return entity;
    }

    private WindowOccurrence occurrence() {
        return new WindowOccurrence(1L, 2L);
    }
}
