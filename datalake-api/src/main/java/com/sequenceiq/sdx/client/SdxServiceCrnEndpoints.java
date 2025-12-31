package com.sequenceiq.sdx.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
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

public class SdxServiceCrnEndpoints extends AbstractUserCrnServiceEndpoint implements SdxClient {

    protected SdxServiceCrnEndpoints(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    @Override
    public SdxInternalEndpoint sdxInternalEndpoint() {
        return getEndpoint(SdxInternalEndpoint.class);
    }

    @Override
    public SdxEndpoint sdxEndpoint() {
        return getEndpoint(SdxEndpoint.class);
    }

    @Override
    public SdxRotationEndpoint sdxRotationEndpoint() {
        return getEndpoint(SdxRotationEndpoint.class);
    }

    @Override
    public SdxUpgradeEndpoint sdxUpgradeEndpoint() {
        return getEndpoint(SdxUpgradeEndpoint.class);
    }

    @Override
    public SdxRecoveryEndpoint sdxRecoveryEndpoint() {
        return getEndpoint(SdxRecoveryEndpoint.class);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public ProgressEndpoint progressEndpoint() {
        return getEndpoint(ProgressEndpoint.class);
    }

    @Override
    public OperationEndpoint operationEndpoint() {
        return getEndpoint(OperationEndpoint.class);
    }

    @Override
    public DiagnosticsEndpoint diagnosticsEndpoint() {
        return getEndpoint(DiagnosticsEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    @Override
    public DatabaseServerEndpoint databaseServerEndpoint() {
        return getEndpoint(DatabaseServerEndpoint.class);
    }

    @Override
    public AuthorizationUtilEndpoint authorizationUtilEndpoint() {
        return getEndpoint(AuthorizationUtilEndpoint.class);
    }

    @Override
    public SdxBackupEndpoint sdxBackupEndpoint() {
        return getEndpoint(SdxBackupEndpoint.class);
    }

    @Override
    public SdxRestoreEndpoint sdxRestoreEndpoint() {
        return getEndpoint(SdxRestoreEndpoint.class);
    }

    @Override
    public CDPStructuredEventV1Endpoint structuredEventsV1Endpoint() {
        return getEndpoint(CDPStructuredEventV1Endpoint.class);
    }

    @Override
    public SdxEventEndpoint sdxEventEndpoint() {
        return getEndpoint(SdxEventEndpoint.class);
    }

    @Override
    public SupportV1Endpoint supportV1Endpoint() {
        return getEndpoint(SupportV1Endpoint.class);
    }
}
