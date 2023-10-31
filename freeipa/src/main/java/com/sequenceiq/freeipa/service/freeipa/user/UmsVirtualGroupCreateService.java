package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@Service
public class UmsVirtualGroupCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsVirtualGroupCreateService.class);

    @Inject
    private VirtualGroupService virtualGroupService;

    public void createVirtualGroups(String accountId, List<StackUserSyncView> stacks) {
        Set<String> environmentCrns = stacks
                .stream()
                .filter(StackUserSyncView::isAvailable)
                .map(StackUserSyncView::environmentCrn)
                .collect(Collectors.toSet());

        LOGGER.info("Sync virtual groups for environments with available stack: {}", environmentCrns);
        environmentCrns.forEach(envCrn -> {
            try {
                Map<UmsVirtualGroupRight, String> virtualGroups = measure(() -> virtualGroupService.createVirtualGroups(accountId, envCrn), LOGGER,
                        "Creating virtual groups took {} ms for env '{}'", envCrn);
                LOGGER.info("Virtual group creation finished for env '{}'. Available virtual groups: '{}'", envCrn, virtualGroups.values());
            } catch (Exception e) {
                LOGGER.error("Virtual group creation failed for env '{}'", envCrn, e);
            }
        });
    }
}
