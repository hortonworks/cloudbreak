package com.sequenceiq.freeipa.cache.cert;

import javax.persistence.Embeddable;

@Embeddable
public class Cert {
    private String cert;

    public Cert() {
    }

    public Cert(String cert) {
        this.cert = cert;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }
}
