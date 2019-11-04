package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Cert;

import spark.Request;
import spark.Response;

@Component
public class CertFindResponse extends AbstractFreeIpaResponse<Set<Cert>> {
    @Override
    public String method() {
        return "cert_find";
    }

    @Override
    protected Set<Cert> handleInternal(Request request, Response response) {
        Cert cert = new Cert();
        cert.setCacn("dummy");
        cert.setIssuer("dummy");
        return Set.of(cert);
    }
}
