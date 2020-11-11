package com.sequenceiq.cloudbreak.usage;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

/**
 * Interface for reporting usage.
 */
public interface UsageReporter {
    /**
     * The overall version of the usage event model. See the comments on the
     * version field in the Event message in usage.proto for details on how this
     * is used.
     *
     * Version history:
     *   1: Initial version.
     *   2: IAM user created and user updated events updated for GDPR compliance.
     *   3: IAM user created events will always have one of identity provider ID
     *        or SFDC contact ID set.
     */
    int USAGE_VERSION = 3;
    /**
     * Reports that an CDP Datahub cluster has been requested.
     * @param timestamp the cluster creation date which will be used as the
     * timestamp for the event
     * @param details the event details
     */
    void cdpDatahubClusterRequested(
            long timestamp,
            UsageProto.CDPDatahubClusterRequested details);

    /**
     * Reports that an CDP Datahub cluster's status has changed.
     * @param details the event details
     */
    void cdpDatahubClusterStatusChanged(
            UsageProto.CDPDatahubClusterStatusChanged details);

    /**
     * Reports that an CDP Datalake cluster has been requested.
     * @param timestamp the cluster creation date which will be used as the
     * timestamp for the event
     * @param details the event details
     */
    void cdpDatalakeClusterRequested(
            long timestamp,
            UsageProto.CDPDatalakeClusterRequested details);

    /**
     * Reports that an CDP Datalake cluster's status has changed.
     * @param details the event details
     */
    void cdpDatalakeClusterStatusChanged(
            UsageProto.CDPDatalakeClusterStatusChanged details);

    /**
     * Reports that a CDP telemetry event has happened.
     * @param details the event details
     */
    void cdpTelemetryEvent(
            UsageProto.CDPTelemetryEvent details);
}
