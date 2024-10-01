package com.sequenceiq.freeipa.controller;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.EncryptionV1Endpoint;
import com.sequenceiq.freeipa.converter.encryption.StackEncryptionToStackEncryptionResponseConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class EncryptionV1Controller implements EncryptionV1Endpoint {

    @Inject
    private CrnService crnService;

    @Inject
    private StackService stackService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private StackEncryptionToStackEncryptionResponseConverter stackEncryptionConverter;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public StackEncryptionResponse getEncryptionKeys(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        Stack stack = stackService.getFreeIpaStackWithMdcContext(environmentCrn, accountId);
        StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stack.getId());
        return stackEncryptionConverter.convert(stackEncryption);
    }
}
