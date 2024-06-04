package com.sequenceiq.cloudbreak.service.rotaterdscert;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackRotateRdsCertificateService {

    @Inject
    private StackService stackService;

    @Inject
    private StackCommonService stackCommonService;

    public StackRotateRdsCertificateV4Response rotateRdsCertificate(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        return new StackRotateRdsCertificateV4Response(RotateRdsCertResponseType.TRIGGERED,
                stackCommonService.rotateRdsCertificate(stack), null, stack.getResourceCrn());
    }
}
