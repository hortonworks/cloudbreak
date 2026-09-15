package com.sequenceiq.environment.environment.verification;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;

/**
 * Renders the failed actions of the tag-update permission check as {@link PolicyValidationErrorResponses}.
 *
 * <p>Shared by the HTTP 400 exception mapper (which returns this in {@code ExceptionResponse.payload}) and by the
 * read-only {@code tag_update_permissions} endpoint (which returns it as the 200 body), so the UI parses one shape
 * regardless of which call produced it.</p>
 *
 * <p>One row per failed action: {@code service="TAG_UPDATE"}, {@code message="<action> : <resource>"} and
 * {@code code=}{@link CDPServicePolicyVerificationResponse#NOT_FOUND}. That per-row {@code code} is <em>not</em> an
 * HTTP status — it is the payload's own "permission missing" marker, matching what
 * {@link PolicyValidationErrorResponseConverter} emits for the internal policy-validation endpoint.</p>
 *
 * <p>When the check failed before it could name any action — the credential may not even be allowed to read its own
 * permissions — there is nothing to list, yet an empty collection is how "everything is granted" is expressed. Such a
 * result therefore becomes a single {@link CDPServicePolicyVerificationResponse#SERVICE_UNAVAILABLE} row carrying the
 * message, so an unverifiable credential is never rendered as a verified one.</p>
 */
public final class TagUpdatePermissionPolicyResponseConverter {

    public static final String TAG_UPDATE_SERVICE = "TAG_UPDATE";

    private static final Integer MISSING_PERMISSION_CODE = CDPServicePolicyVerificationResponse.NOT_FOUND;

    private static final Integer CHECK_FAILED_CODE = CDPServicePolicyVerificationResponse.SERVICE_UNAVAILABLE;

    private TagUpdatePermissionPolicyResponseConverter() {
    }

    public static PolicyValidationErrorResponses convert(String message, List<String> failedActions) {
        if (failedActions.isEmpty()) {
            return new PolicyValidationErrorResponses(message == null ? List.of() : List.of(row(message, CHECK_FAILED_CODE)));
        }
        return new PolicyValidationErrorResponses(failedActions.stream()
                .map(failedAction -> row(failedAction, MISSING_PERMISSION_CODE))
                .toList());
    }

    private static PolicyValidationErrorResponse row(String message, Integer code) {
        PolicyValidationErrorResponse row = new PolicyValidationErrorResponse();
        row.setService(TAG_UPDATE_SERVICE);
        row.setMessage(message);
        row.setCode(code);
        return row;
    }
}
