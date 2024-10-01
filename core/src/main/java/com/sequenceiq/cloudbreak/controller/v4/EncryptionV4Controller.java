package com.sequenceiq.cloudbreak.controller.v4;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.encryption.EncryptionV4Endpoint;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackEncryptionToStackEncryptionResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;

@Controller
public class EncryptionV4Controller implements EncryptionV4Endpoint {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private StackEncryptionToStackEncryptionResponseConverter stackEncryptionConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackEncryptionResponse getEncryptionKeys(@ResourceCrn String crn) {
        StackDto stack = stackDtoService.getByCrnWithMdcContext(crn);
        StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stack.getId());
        return stackEncryptionConverter.convert(stackEncryption);
    }
}
