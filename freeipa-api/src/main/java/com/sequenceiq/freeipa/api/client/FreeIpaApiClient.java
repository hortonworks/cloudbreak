package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;

public interface FreeIpaApiClient {
    FreeIpaV1Endpoint getFreeIpaV1Endpoint();

    LdapConfigV1Endpoint getLdapConfigV1Endpoint();

    KerberosConfigV1Endpoint getKerberosConfigV1Endpoint();

    UserV1Endpoint getUsersyncV1Endpoint();

    ClientTestV1Endpoint getClientTestV1Endpoint();
}
