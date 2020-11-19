package com.sequenceiq.mock.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Cert;

@Component
public class CertFindResponse extends AbstractFreeIpaResponse<Set<Cert>> {
    @Override
    public String method() {
        return "cert_find";
    }

    @Override
    protected Set<Cert> handleInternal(String body) {
        Cert cert = new Cert();
        cert.setCacn("dummy");
        cert.setIssuer("dummy");
        return Set.of(cert);
    }
}
