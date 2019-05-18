package com.sequenceiq.environment.credential.attributes.gcp;

public class GcpCredentialAttributes {

    private P12Attributes p12;

    private JsonAttributes json;

    public P12Attributes getP12() {
        return p12;
    }

    public void setP12(P12Attributes p12) {
        this.p12 = p12;
    }

    public JsonAttributes getJson() {
        return json;
    }

    public void setJson(JsonAttributes json) {
        this.json = json;
    }
}
