package com.sequenceiq.freeipa.api.client;

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

public interface FreeIpaClient {

    FreeIpaV1Endpoint getFreeIpaV1Endpoint();

    LdapConfigV1Endpoint getLdapConfigV1Endpoint();

    KerberosConfigV1Endpoint getKerberosConfigV1Endpoint();

    UserV1Endpoint getUserV1Endpoint();

    ClientTestV1Endpoint getClientTestV1Endpoint();

    KerberosMgmtV1Endpoint getKerberosMgmtV1Endpoint();

    DnsV1Endpoint getDnsV1Endpoint();

    OperationV1Endpoint getOperationV1Endpoint();

    DiagnosticsV1Endpoint getDiagnosticsEndpoint();

    FlowEndpoint getFlowEndpoint();

    FlowPublicEndpoint getFlowPublicEndpoint();

    CDPStructuredEventV1Endpoint structuredEventsV1Endpoint();

    UtilV1Endpoint utilV1Endpoint();
}
