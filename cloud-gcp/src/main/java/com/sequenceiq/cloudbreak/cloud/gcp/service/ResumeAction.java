package com.sequenceiq.cloudbreak.cloud.gcp.service;

/**
 * Where a GCP disk-type-change migration should (re)start after a possible mid-flight JVM death. The migration is a
 * six-step sequence (snapshot &rarr; create new disk &rarr; detach old &rarr; attach new &rarr; delete old &rarr; delete
 * snapshot); a rerun re-derives both the new disk and snapshot names deterministically and branches on the actual
 * attachment state of the disks so the old disk is never deleted while the new one is not yet attached.
 */
public enum ResumeAction {

    /** No usable new disk exists yet: run all six steps from the start. */
    FULL_MIGRATION,

    /** The new disk is READY and unattached but the old disk is still attached: resume at the detach step (steps 3&ndash;6). */
    RESUME_AT_DETACH,

    /** The new disk is READY and unattached and the old disk is already detached: resume at the attach step (steps 4&ndash;6). */
    RESUME_AT_ATTACH,

    /** The new disk is already attached to the target instance: the swap completed earlier, only cleanup remains (delete old + snapshot). */
    CLEANUP_ONLY,

    /** A leftover new disk of ours exists but is not usable (not READY): delete it, then run the full migration. */
    RECREATE_NEW_DISK
}
