package com.sequenceiq.cloudbreak.sdx.pdl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDeleteService;

@Service
public class PdlSdxDeleteService extends PdlSdxStatusService implements PlatformAwareSdxDeleteService<PrivateDatalakeDetails.StatusEnum> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxDeleteService.class);

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        LOGGER.info(String.format("DL with CRN: %s is Private so it should not be deleted.", sdxCrn));
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(PrivateDatalakeDetails.StatusEnum status) {
        return PollingResult.COMPLETED;
    }
}