package com.sequenceiq.cloudbreak.sdx.pdl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDhTearDownService;

@Service
public class PdlSdxDhTearDownService extends AbstractPdlSdxService implements PlatformAwareSdxDhTearDownService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxDhTearDownService.class);

    @Override
    public void tearDownDataHub(String sdxCrn, String datahubCrn) {
        LOGGER.info("DH teardown is not yet implemented in PDL service.");
    }
}
