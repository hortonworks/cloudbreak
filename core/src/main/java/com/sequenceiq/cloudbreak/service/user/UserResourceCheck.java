package com.sequenceiq.cloudbreak.service.user;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Service
public class UserResourceCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResourceCheck.class);

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private NetworkRepository networkRepository;

    @Transactional(readOnly = true)
    public boolean hasResources(IdentityUser admin, String userId) {
        IdentityUser user = userDetailsService.getDetails(userId, UserFilterField.USERID);
        LOGGER.info("{} / {} checks resources of {}", admin.getUserId(), admin.getUsername(), userId);
        String errorMessage = null;
        if (!admin.getRoles().contains(IdentityUserRole.ADMIN)) {
            errorMessage = "Forbidden: user (%s) is not authorized for this operation on %s";
        }
        if (!admin.getAccount().equals(user.getAccount())) {
            errorMessage = "Forbidden: admin (%s) and user (%s) are not under the same account.";
        }
        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw new AccessDeniedException(String.format(errorMessage, admin.getUsername(), user.getUsername()));
        }
        Set<Template> templates = templateRepository.findForUser(user.getUserId());
        Set<Credential> credentials = credentialRepository.findForUser(user.getUserId());
        Set<Blueprint> blueprints = blueprintRepository.findForUser(user.getUserId());
        Set<Network> networks = networkRepository.findForUser(user.getUserId());
        Set<Stack> stacks = stackRepository.findForUser(user.getUserId());
        return !(stacks.isEmpty() && templates.isEmpty() && credentials.isEmpty()
                && blueprints.isEmpty() && networks.isEmpty());
    }

}
