package com.sequenceiq.environment.environment.validation.validators;

import java.util.List;

/**
 * Outcome of the tag-update permission check.
 *
 * <p>Distinguishes the three states the callers have to render differently:</p>
 * <ul>
 *     <li>{@link #granted()} — every required action is available (or the platform has no validator registered), so
 *     tag propagation can be enabled. {@code message} is {@code null} and {@code failedActions} is empty.</li>
 *     <li>permissions are missing — {@code failedActions} lists them and {@code message} explains it.</li>
 *     <li>the check could not run — the credential lacks {@code iam:SimulatePrincipalPolicy}, listing the Azure role
 *     assignments failed, GCP {@code testIamPermissions} raised an {@link java.io.IOException}, ... Here
 *     {@code message} is set while {@code failedActions} is empty, which is what {@link #verifiable()} reports.
 *     A credential we could not verify must not be shown to the user as a verified one.</li>
 * </ul>
 *
 * <p>{@code cause} is kept so the throwing caller can preserve the original cloud SDK stack trace; the reporting
 * caller ignores it.</p>
 */
public record TagUpdatePermissionResult(String message, List<String> failedActions, Throwable cause) {

    public TagUpdatePermissionResult(String message, List<String> failedActions, Throwable cause) {
        this.message = message;
        this.failedActions = failedActions == null ? List.of() : List.copyOf(failedActions);
        this.cause = cause;
    }

    public static TagUpdatePermissionResult granted() {
        return new TagUpdatePermissionResult(null, List.of(), null);
    }

    public static TagUpdatePermissionResult error(String message, List<String> failedActions, Throwable cause) {
        return new TagUpdatePermissionResult(message, failedActions, cause);
    }

    public boolean hasError() {
        return message != null;
    }

    /**
     * @return false when the check itself could not be performed, so neither "granted" nor a concrete list of missing
     *         actions can be claimed. The individual validators report this by leaving the failed actions empty.
     */
    public boolean verifiable() {
        return !hasError() || !failedActions.isEmpty();
    }
}
