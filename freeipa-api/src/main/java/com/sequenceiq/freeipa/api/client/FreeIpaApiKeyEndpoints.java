package com.sequenceiq.freeipa.api.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.freeipa.api.v1.diagnostics.DiagnosticsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;

public class FreeIpaApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements FreeIpaClient {

    FreeIpaApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public FreeIpaV1Endpoint getFreeIpaV1Endpoint() {
        return getEndpoint(FreeIpaV1Endpoint.class);
    }

    @Override
    public LdapConfigV1Endpoint getLdapConfigV1Endpoint() {
        return getEndpoint(LdapConfigV1Endpoint.class);
    }

    @Override
    public KerberosConfigV1Endpoint getKerberosConfigV1Endpoint() {
        return getEndpoint(KerberosConfigV1Endpoint.class);
    }

    @Override
    public UserV1Endpoint getUserV1Endpoint() {
        return getEndpoint(UserV1Endpoint.class);
    }

    @Override
    public ClientTestV1Endpoint getClientTestV1Endpoint() {
        return getEndpoint(ClientTestV1Endpoint.class);
    }

    @Override
    public KerberosMgmtV1Endpoint getKerberosMgmtV1Endpoint() {
        return getEndpoint(KerberosMgmtV1Endpoint.class);
    }

    @Override
    public DnsV1Endpoint getDnsV1Endpoint() {
        return getEndpoint(DnsV1Endpoint.class);
    }

    @Override
    public OperationV1Endpoint getOperationV1Endpoint() {
        return getEndpoint(OperationV1Endpoint.class);
    }

    @Override
    public DiagnosticsV1Endpoint getDiagnosticsEndpoint() {
        return getEndpoint(DiagnosticsV1Endpoint.class);
    }

    @Override
    public FlowEndpoint getFlowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint getFlowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    @Override
    public CDPStructuredEventV1Endpoint structuredEventsV1Endpoint() {
        return getEndpoint(CDPStructuredEventV1Endpoint.class);
    }

    @Override
    public UtilV1Endpoint utilV1Endpoint() {
        return getEndpoint(UtilV1Endpoint.class);
    }
}
