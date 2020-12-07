package com.sequenceiq.freeipa.service.freeipa.dns;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.FreeIpa;

public class FreeIpaAndClient {

    private final FreeIpa freeIpa;

    private final FreeIpaClient client;

    public FreeIpaAndClient(FreeIpa freeIpa, FreeIpaClient client) {
        this.freeIpa = freeIpa;
        this.client = client;
    }

    public FreeIpa getFreeIpa() {
        return freeIpa;
    }

    public FreeIpaClient getClient() {
        return client;
    }
}
