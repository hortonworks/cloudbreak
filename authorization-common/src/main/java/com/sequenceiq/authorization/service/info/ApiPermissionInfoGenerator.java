package com.sequenceiq.authorization.service.info;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.info.model.ApiAuthorizationInfo;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@Service
public class ApiPermissionInfoGenerator {

    private static final String RBAC_DOC_MESSAGE_KEY = "authorization.doc";

    @Inject
    private CloudbreakMessagesService messagesService;

    public Set<ApiAuthorizationInfo> generateApiMethodsWithRequiredPermission() {
        ApiAuthorizationInfo authInfo = new ApiAuthorizationInfo();
        authInfo.setMessage(messagesService.getMessage(RBAC_DOC_MESSAGE_KEY));
        return Set.of(authInfo);
    }
}
