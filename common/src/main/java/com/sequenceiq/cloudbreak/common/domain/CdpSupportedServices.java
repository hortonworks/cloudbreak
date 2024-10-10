package com.sequenceiq.cloudbreak.common.domain;

import java.util.Locale;
import java.util.Set;

public enum CdpSupportedServices {

    ENVIRONMENT,
    DATAHUB,
    CDW,
    CML,
    CDF,
    CDE,
    COD,
    ALL(ENVIRONMENT, DATAHUB, CDW, CML, CDF, CDE, COD);

    private final Set<CdpSupportedServices> services;

    CdpSupportedServices(CdpSupportedServices... services) {
        this.services = services == null ? Set.of() : Set.of(services);
    }

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Set<CdpSupportedServices> services() {
        return services.isEmpty() ? Set.of(this) : services;
    }
}
