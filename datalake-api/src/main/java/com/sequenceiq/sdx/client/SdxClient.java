package com.sequenceiq.sdx.client;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.sdx.api.endpoint.DatabaseServerEndpoint;
import com.sequenceiq.sdx.api.endpoint.DiagnosticsEndpoint;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.ProgressEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxBackupEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRecoveryEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRestoreEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.endpoint.SupportV1Endpoint;

public interface SdxClient {

    SdxInternalEndpoint sdxInternalEndpoint();

    SdxEndpoint sdxEndpoint();

    SdxRotationEndpoint sdxRotationEndpoint();

    SdxUpgradeEndpoint sdxUpgradeEndpoint();

    SdxRecoveryEndpoint sdxRecoveryEndpoint();

    FlowEndpoint flowEndpoint();

    ProgressEndpoint progressEndpoint();

    OperationEndpoint operationEndpoint();

    FlowPublicEndpoint flowPublicEndpoint();

    DiagnosticsEndpoint diagnosticsEndpoint();

    DatabaseServerEndpoint databaseServerEndpoint();

    AuthorizationUtilEndpoint authorizationUtilEndpoint();

    SdxBackupEndpoint sdxBackupEndpoint();

    SdxRestoreEndpoint sdxRestoreEndpoint();

    CDPStructuredEventV1Endpoint structuredEventsV1Endpoint();

    SdxEventEndpoint sdxEventEndpoint();

    SupportV1Endpoint supportV1Endpoint();
}
