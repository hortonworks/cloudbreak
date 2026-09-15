package com.sequenceiq.environment.exception.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.exception.EnvironmentTagUpdatePermissionMissingException;

public class EnvironmentTagUpdatePermissionMissingExceptionMapperTest {

    private final EnvironmentTagUpdatePermissionMissingExceptionMapper underTest = new EnvironmentTagUpdatePermissionMissingExceptionMapper();

    @Test
    void mapsToBadRequestWithMessageAndPerActionPayload() {
        List<String> failedActions = List.of("ec2:CreateTags : *", "kms:TagResource : *");
        EnvironmentTagUpdatePermissionMissingException exception = new EnvironmentTagUpdatePermissionMissingException(
                "missing tag actions", failedActions, null);

        Object entity;
        try (Response response = underTest.toResponse(exception)) {
            assertThat(response.getStatusInfo()).isEqualTo(Status.BAD_REQUEST);
            entity = response.getEntity();
        }
        assertThat(entity).isInstanceOf(ExceptionResponse.class);
        ExceptionResponse body = (ExceptionResponse) entity;
        assertThat(body.getMessage()).isEqualTo("missing tag actions");
        assertThat(body.getPayload()).isInstanceOf(PolicyValidationErrorResponses.class);
        PolicyValidationErrorResponses payload = (PolicyValidationErrorResponses) body.getPayload();
        assertThat(payload.getResponses()).hasSize(2);
        assertThat(payload.getResponses()).allSatisfy(row -> {
            assertThat(row.getService()).isEqualTo("TAG_UPDATE");
            assertThat(row.getCode()).isEqualTo(404);
        });
        assertThat(payload.getResponses().stream().map(PolicyValidationErrorResponse::getMessage))
                .containsExactlyInAnyOrderElementsOf(failedActions);
    }

    @Test
    void emptyFailedActionsIsReportedAsACheckFailureRowRatherThanAnEmptyCollection() {
        EnvironmentTagUpdatePermissionMissingException exception = new EnvironmentTagUpdatePermissionMissingException(
                "could not verify the credential", List.of(), null);

        ExceptionResponse body;
        try (Response response = underTest.toResponse(exception)) {
            body = (ExceptionResponse) response.getEntity();
        }
        PolicyValidationErrorResponses payload = (PolicyValidationErrorResponses) body.getPayload();
        assertThat(payload.getResponses()).hasSize(1);
        PolicyValidationErrorResponse row = payload.getResponses().iterator().next();
        assertThat(row.getService()).isEqualTo("TAG_UPDATE");
        assertThat(row.getMessage()).isEqualTo("could not verify the credential");
        assertThat(row.getCode()).isEqualTo(503);
    }
}
