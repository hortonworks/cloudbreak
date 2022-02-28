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
     * Reports that an CDP Environment has been requested.
     * @param details the event details
     */
    void cdpEnvironmentRequested(
            UsageProto.CDPEnvironmentRequested details);

    /**
     * Reports that an CDP Environment status has been changed.
     * @param details the event status details
     */
    void cdpEnvironmentStatusChanged(
            UsageProto.CDPEnvironmentStatusChanged details);

    /**
     * Reports that an CDP Datalake has been requested.
     * @param details the event details
     */
    void cdpDatalakeRequested(
            UsageProto.CDPDatalakeRequested details);

    /**
     * Reports that an CDP Datalake status has been changed.
     * @param details the event status details
     */
    void cdpDatalakeStatusChanged(
            UsageProto.CDPDatalakeStatusChanged details);

    /**
     * Reports that an CDP Datahub has been requested.
     * @param details the event details
     */
    void cdpDatahubRequested(
            UsageProto.CDPDatahubRequested details);

    /**
     * Reports that an CDP Datahub status has been changed.
     * @param details the event status details
     */
    void cdpDatahubStatusChanged(
            UsageProto.CDPDatahubStatusChanged details);

    /**
     * Reports that an CDP Datalake has been periodically synced.
     * @param details the event details
     */
    void cdpDatalakeSync(
            UsageProto.CDPDatalakeSync details);

    /**
     * Reports that an CDP Datahub has been periodically synced.
     * @param details the event details
     */
    void cdpDatahubSync(
            UsageProto.CDPDatahubSync details);

    /**
     * Reports that an CDP Datahub Autoscale has been triggered.
     * @param details the event details
     */
    void cdpDatahubAutoscaleTriggered(
            UsageProto.CDPDatahubAutoscaleTriggered details);

    /**
     * Reports that CDP Datahub Autoscale config changed.
     * @param details the event details
     */
    void cdpDatahubAutoscaleConfigChanged(
            UsageProto.CDPDatahubAutoscaleConfigChanged details);

    /**
     * Reports CDP datalake/datahub network check results.
     * @param details the event details
     */
    void cdpNetworkCheckEvent(
            UsageProto.CDPNetworkCheck details);

    /**
     * Reports CDP VM diagnostics results.
     * @param details the event details
     */
    void cdpVmDiagnosticsEvent(
            UsageProto.CDPVMDiagnosticsEvent details);
}
