package com.sequenceiq.cloudbreak.api.model;

public enum SssdTlsReqcert {

    NEVER, ALLOW, TRY, DEMAND, HARD;

    public String getRepresentation() {
        return name().toLowerCase();
    }

    public static SssdTlsReqcert fromString(String representation) {
        for (SssdTlsReqcert cert : SssdTlsReqcert.values()) {
            if (cert.name().equalsIgnoreCase(representation)) {
                return cert;
            }
        }
        return null;
    }
}
