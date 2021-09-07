package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MultiAzConverter {

    @Value("${cb.multiaz.supported.variants:AWS_NATIVE}")
    private Set<String> supportedMultiAzVariants;

    public boolean convert(String variant) {
        return supportedMultiAzVariants.contains(variant);
    }
}
