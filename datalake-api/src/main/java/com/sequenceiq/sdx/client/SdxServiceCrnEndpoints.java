package com.sequenceiq.sdx.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.sdx.api.endpoint.DatabaseServerEndpoint;
import com.sequenceiq.sdx.api.endpoint.DiagnosticsEndpoint;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.ProgressEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;

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
    public SdxUpgradeEndpoint sdxUpgradeEndpoint() {
        return getEndpoint(SdxUpgradeEndpoint.class);
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
}
