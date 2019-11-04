package com.sequenceiq.cloudbreak.freeipa;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeIpaAdapter {

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    public String getDomain(String environmentCrn) {
        DescribeFreeIpaResponse freeIpaResponse = freeIpaV1Endpoint.describe(environmentCrn);
        return freeIpaResponse.getFreeIpa().getDomain();
    }
}
