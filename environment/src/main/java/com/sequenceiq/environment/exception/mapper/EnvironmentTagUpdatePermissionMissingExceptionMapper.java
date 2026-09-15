package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.environment.verification.TagUpdatePermissionPolicyResponseConverter;
import com.sequenceiq.environment.exception.EnvironmentTagUpdatePermissionMissingException;

/**
 * Renders {@link EnvironmentTagUpdatePermissionMissingException} as HTTP 400 and packs the individual
 * missing IAM actions into {@code ExceptionResponse.payload} as a {@link PolicyValidationErrorResponses}
 * so the UI can render the list without parsing the flattened message string.
 *
 * <p>The payload is built by {@link TagUpdatePermissionPolicyResponseConverter}, the same converter the read-only
 * {@code tag_update_permissions} endpoint uses, so the UI parses one shape either way. The HTTP status of this mapper
 * is 400, see {@link #getResponseStatus}.</p>
 */
@Provider
@Component
public class EnvironmentTagUpdatePermissionMissingExceptionMapper
        extends EnvironmentBaseExceptionMapper<EnvironmentTagUpdatePermissionMissingException> {

    @Override
    public Status getResponseStatus(EnvironmentTagUpdatePermissionMissingException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<EnvironmentTagUpdatePermissionMissingException> getExceptionType() {
        return EnvironmentTagUpdatePermissionMissingException.class;
    }

    @Override
    protected Object getPayload(EnvironmentTagUpdatePermissionMissingException exception) {
        return TagUpdatePermissionPolicyResponseConverter.convert(exception.getMessage(), exception.getFailedActions());
    }
}
