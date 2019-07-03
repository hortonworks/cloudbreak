package com.sequenceiq.freeipa.api.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;

public class FreeIpaApiUserCrnEndpoint extends AbstractUserCrnServiceEndpoint implements FreeIpaApiEndpoint {
    public FreeIpaApiUserCrnEndpoint(WebTarget webTarget, String crn) {
        super(webTarget, crn);
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
}
