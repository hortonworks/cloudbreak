package com.sequenceiq.cloudbreak.cloud.model;

/**
 * The outcome of updating a single disk volume, keyed back to the volume's original id. Providers that change a
 * volume's type in place (AWS, Azure) do not rename anything, so they report no results; providers that recreate the
 * underlying disk under a new name (GCP disk-type change) report the new id, device, and provider-authoritative size
 * so the caller can persist the rename without relying on the connector mutating shared objects in place.
 *
 * @param oldVolumeId the volume's id before the update (used to match it back to the persisted volume)
 * @param newVolumeId the volume's id after the update (equal to {@code oldVolumeId} when it was not renamed)
 * @param newDevice   the volume's device name after the update
 * @param newSize     the volume's size in GB after the update, as reported by the provider (may differ from the
 *                    requested size, e.g. GCP floors a type change at the current disk size)
 */
public record VolumeUpdateResult(
        String oldVolumeId,
        String newVolumeId,
        String newDevice,
        Integer newSize
) {
}
