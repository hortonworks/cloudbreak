package com.sequenceiq.cloudbreak.common.json.patch;

/**
 * Thrown when a JSON Patch {@code test} operation does not match the current value in the base document.
 * In the runtime-overlay model this signals that a base template drifted underneath a patch that was
 * authored against an older base, so the overlay must be re-reviewed instead of silently producing a
 * wrong template.
 */
public class JsonPatchTestFailedException extends RuntimeException {

    public JsonPatchTestFailedException(String message) {
        super(message);
    }
}
