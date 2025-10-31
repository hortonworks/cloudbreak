package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.freeipa.api.v1.freeipa.crossrealm.TrustV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.KerberosMgmtV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.progress.ProgressV1Endpoint;
import com.sequenceiq.freeipa.api.v1.recipe.RecipeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.FreeIpaV2Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.crossrealm.TrustV2Endpoint;

public interface FreeIpaApiEndpoint {
    FreeIpaV1Endpoint getFreeIpaV1Endpoint();

    TrustV1Endpoint getCrossRealmV1Endpoint();

    TrustV2Endpoint getCrossRealmV2Endpoint();

    FreeIpaV2Endpoint getFreeIpaV2Endpoint();

    FreeIpaRotationV1Endpoint getFreeipaRotationV1Endpoint();

    LdapConfigV1Endpoint getLdapConfigV1Endpoint();

    KerberosConfigV1Endpoint getKerberosConfigV1Endpoint();

    UserV1Endpoint getUserV1Endpoint();

    ClientTestV1Endpoint getClientTestV1Endpoint();

    KerberosMgmtV1Endpoint getKerberosMgmtV1Endpoint();

    OperationV1Endpoint getOperationV1Endpoint();

    ProgressV1Endpoint getProgressV1Endpoint();

    UtilV1Endpoint getUtilV1Endpoint();

    RecipeV1Endpoint getRecipeV1Endpoint();

    FreeIpaV1FlowEndpoint getFreeIpaV1FlowEndpoint();
}
