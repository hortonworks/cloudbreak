package com.sequenceiq.redbeams.service.store;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.api.model.common.Status;

@Service
public class RedbeamsInMemoryStateStoreUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsInMemoryStateStoreUpdaterService.class);

    @Inject
    private RedbeamsInMemoryStateStoreService redbeamsInMemoryStateStoreService;

    public void update(Long id, Status newStatus) {
        LOGGER.info("Update redbems flow state in the memory state by new status: {}", newStatus);
        if (newStatus.isSuccessfullyDeleted()) {
            redbeamsInMemoryStateStoreService.delete(id);
        } else if (newStatus.isDeleteInProgress()) {
            redbeamsInMemoryStateStoreService.registerCancel(id);
        } else {
            redbeamsInMemoryStateStoreService.registerStart(id);
        }
    }
}
