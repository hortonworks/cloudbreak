package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.REPAIR_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.FreeIpaV2Endpoint;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.entity.Stack;

@Controller
@AccountEntityType(Stack.class)
public class FreeIpaV2Controller implements FreeIpaV2Endpoint {
    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public DescribeFreeIpaResponse rebuildv2(@RequestObject @Valid RebuildV2Request request) throws Exception {
        throw new UnsupportedOperationException("Operation not implemented");
    }
}
