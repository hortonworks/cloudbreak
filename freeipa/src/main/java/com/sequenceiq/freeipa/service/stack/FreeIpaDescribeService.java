package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncStatusService;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class FreeIpaDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDescribeService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CachedEnvironmentClientService environmentService;

    public DescribeFreeIpaResponse describe(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        DetailedEnvironmentResponse environmentResponse = environmentService.getByCrn(environmentCrn);
        DescribeFreeIpaResponse response = getResponseForStack(stack, false, environmentResponse);
        LOGGER.trace("FreeIPA describe response: {}", response);
        return response;
    }

    public List<DescribeFreeIpaResponse> describeAll(String environmentCrn, String accountId) {
        if (!entitlementService.isFreeIpaRebuildEnabled(accountId)) {
            throw new BadRequestException("The FreeIPA rebuild capability is disabled.");
        }
        List<Stack> stacks = stackService.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(environmentCrn, accountId);
        DetailedEnvironmentResponse environmentResponse = environmentService.getByCrn(environmentCrn);
        List<DescribeFreeIpaResponse> response = stacks.stream()
                .map(s -> getResponseForStack(s, true, environmentResponse))
                .collect(Collectors.toList());
        LOGGER.trace("FreeIPA describe all response: {}", response);
        return response;
    }

    public Image getImage(String environmentCrn, String accountId) {
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        return imageService.getImageForStack(stack);
    }

    private DescribeFreeIpaResponse getResponseForStack(Stack stack, Boolean includeAllInstances, DetailedEnvironmentResponse environmentResponse) {
        MDCBuilder.buildMdcContext(stack);
        ImageEntity image = imageService.getByStack(stack);
        FreeIpa freeIpa = freeIpaService.findByStackId(stack.getId());
        Optional<UserSyncStatus> userSyncStatus = userSyncStatusService.findByStack(stack);
        return stackToDescribeFreeIpaResponseConverter.convert(stack, image, freeIpa, userSyncStatus, includeAllInstances, environmentResponse);
    }
}
