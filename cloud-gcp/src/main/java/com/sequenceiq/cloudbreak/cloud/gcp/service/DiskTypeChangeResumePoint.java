package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.Optional;

import com.google.api.services.compute.model.Disk;

/**
 * Result of resolving where a GCP disk-type-change migration should resume. {@code existingNewDisk} carries the
 * provider-authoritative new disk (its {@code sizeGb}) when one was already created by a prior attempt, so the caller
 * can report the correct size without a second read; it is empty for {@link ResumeAction#FULL_MIGRATION}.
 */
public record DiskTypeChangeResumePoint(ResumeAction action, Optional<Disk> existingNewDisk) {
}
