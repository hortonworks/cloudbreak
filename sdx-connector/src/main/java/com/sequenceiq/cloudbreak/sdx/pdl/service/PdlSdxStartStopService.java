package com.sequenceiq.cloudbreak.sdx.pdl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;

@Service
public class PdlSdxStartStopService extends AbstractPdlSdxService implements PlatformAwareSdxStartStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxStartStopService.class);

    @Override
    public void startSdx(String sdxCrn) {
        LOGGER.info(String.format("datalake with CRN %s is private so should not be started", sdxCrn));
    }

    @Override
    public void stopSdx(String sdxCrn) {
        LOGGER.info(String.format("datalake with CRN %s is private so should not be stopped", sdxCrn));
    }
}
