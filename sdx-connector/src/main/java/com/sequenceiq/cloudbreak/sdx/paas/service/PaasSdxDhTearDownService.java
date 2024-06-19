package com.sequenceiq.cloudbreak.sdx.paas.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDhTearDownService;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasDhTearDownService;

@Service
public class PaasSdxDhTearDownService extends AbstractPaasSdxService implements PlatformAwareSdxDhTearDownService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxDhTearDownService.class);

    @Inject
    private Optional<LocalPaasDhTearDownService> localPaasDhTearDownService;

    @Override
    public void tearDownDataHub(String sdxCrn, String datahubCrn) {
        localPaasDhTearDownService.ifPresentOrElse(dhTearDownService -> dhTearDownService.tearDownDataHub(sdxCrn, datahubCrn),
                () -> LOGGER.info("DH teardown is not implemented in this service."));
    }
}
