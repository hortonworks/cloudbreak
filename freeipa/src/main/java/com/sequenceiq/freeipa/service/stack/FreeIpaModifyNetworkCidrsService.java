package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaModifyNetworkCidrsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaModifyNetworkCidrsService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    public void modifyNetworkCidrs(String environmentCrn, String accountId, List<String> networkCidrs) {
        LOGGER.info("Modifying network CIDRs for FreeIPA stack of environment {} to {}", environmentCrn, networkCidrs);
        Stack freeIpaStack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        stackUpdater.updateNetworkCidrs(freeIpaStack, networkCidrs);
    }
}
