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
     * Reports that a CDP Datahub cluster has been requested.
     * @param timestamp the cluster creation date which will be used as the
     * timestamp for the event
     * @param details the event details
     */
    void cdpDatahubClusterRequested(
            long timestamp,
            UsageProto.CDPDatahubClusterRequested details);

    /**
     * Reports that a CDP Datahub cluster's status has changed.
     * @param details the event details
     */
    void cdpDatahubClusterStatusChanged(
            UsageProto.CDPDatahubClusterStatusChanged details);

    /**
     * Reports that a CDP Datalake cluster has been requested.
     * @param timestamp the cluster creation date which will be used as the
     * timestamp for the event
     * @param details the event details
     */
    void cdpDatalakeClusterRequested(
            long timestamp,
            UsageProto.CDPDatalakeClusterRequested details);

    /**
     * Reports that a CDP Datalake cluster's status has changed.
     * @param details the event details
     */
    void cdpDatalakeClusterStatusChanged(
            UsageProto.CDPDatalakeClusterStatusChanged details);

    /**
     * Reports that a CDP Environment has been requested.
     * @param details the event details
     */
    void cdpEnvironmentRequested(
            UsageProto.CDPEnvironmentRequested details);

    /**
     * Reports that a CDP Environment status has been changed.
     * @param details the event status details
     */
    void cdpEnvironmentStatusChanged(
            UsageProto.CDPEnvironmentStatusChanged details);

    /**
     * Reports that a CDP FreeIPA status has been changed.
     * @param details the event status details
     */
    void cdpFreeIpaStatusChanged(
            UsageProto.CDPFreeIPAStatusChanged details);

    /**
     * Reports that a CDP Datalake has been requested.
     * @param details the event details
     *
     * @deprecated Contains a subset of the cdpDatalakeStatusChanged data, use cdpDatalakeClusterRequested instead.
     */
    @Deprecated()
    void cdpDatalakeRequested(
            UsageProto.CDPDatalakeRequested details);

    /**
     * Reports that a CDP Datalake status has been changed.
     * @param details the event status details
     */
    void cdpDatalakeStatusChanged(
            UsageProto.CDPDatalakeStatusChanged details);

    /**
     * Reports that a CDP Datahub has been requested.
     * @param details the event details
     *
     * @deprecated Contains a subset of the cdpDatahubStatusChanged data, use cdpDatahubClusterRequested instead.
     */
    @Deprecated
    void cdpDatahubRequested(
            UsageProto.CDPDatahubRequested details);

    /**
     * Reports that a CDP Datahub status has been changed.
     * @param details the event status details
     */
    void cdpDatahubStatusChanged(
            UsageProto.CDPDatahubStatusChanged details);

    /**
     * Reports that a CDP Datalake has been periodically synced.
     * @param details the event details
     */
    void cdpDatalakeSync(
            UsageProto.CDPDatalakeSync details);

    /**
     * Reports that a CDP Datahub has been periodically synced.
     * @param details the event details
     */
    void cdpDatahubSync(
            UsageProto.CDPDatahubSync details);

    /**
     * Reports that a CDP Datahub Autoscale has been triggered.
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

    /**
     * Reports CDP diagnostics event.
     * @param details the event details
     */
    void cdpDiagnosticsEvent(
            UsageProto.CDPDiagnosticEvent details);

    /**
     * Reports a CDP stack patcher event.
     * @param details the event details
     */
    void cdpStackPatcherEvent(
            UsageProto.CDPStackPatchEvent details);

    /**
     * Reports a CDP stack salt password rotation event.
     * @param details the event details
     */
    void cdpSaltPasswordRotationEvent(
            UsageProto.CDPSaltPasswordRotationEvent details);

    /**
     * Reports a recipe lifecycle event (creation/attachment/detachment/deletion)
     * @param details the event details
     */
    void cdpRecipeEvent(
            UsageProto.CDPRecipeEvent details);

    /**
     * Reports a cluster creation event from recipe point of view
     * @param details the event details
     */
    void cdpClusterCreationRecipeEvent(
            UsageProto.CDPClusterCreationRecipeEvent details);

    /**
     * Reports an event for an environment's proxy config is edited
     * @param details the event details
     */
    void cdpEnvironmentProxyConfigEditEvent(
            UsageProto.CDPEnvironmentProxyConfigEditEvent details);

    /**
     * Reports a secret rotation event
     * @param details the event details
     */
    void cdpSecretRotationEvent(
            UsageProto.CDPSecretRotationEvent details);

    /**
     * Reports that a CDP FreeIPA has been periodically synced.
     * @param details the event details
     */
    void cdpFreeipaSync(
            UsageProto.CDPFreeIPASync details);

    /**
     * Reports that a CDP Environment has been periodically synced.
     * @param details the event details
     */
    void cdpEnvironmentSync(
            UsageProto.CDPEnvironmentSync details);

    /**
     * Reports a flow event
     * @param details the event details
     */
    void cdpCloudbreakFlowEvent(UsageProto.CDPCloudbreakFlowEvent details);
}
