package com.sequenceiq.redbeams.events;

/**
 * Payload of the UI notification raised when an external database is running low on storage.
 *
 * @param resourceCrn            the CRN of the external database (DB stack)
 * @param name                   the name of the external database
 * @param environmentCrn         the CRN of the environment the database belongs to
 * @param freeStoragePercentage  the percentage (0-100) of allocated storage still free when the alert was raised
 */
public record ExternalDatabaseStorageAlert(
        String resourceCrn,
        String name,
        String environmentCrn,
        Double freeStoragePercentage) {
}
