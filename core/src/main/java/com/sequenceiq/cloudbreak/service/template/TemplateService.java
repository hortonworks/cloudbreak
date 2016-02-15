package com.sequenceiq.cloudbreak.service.template;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class TemplateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);
    private static final String DELIMITER = "_";

    private static final String TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found.";

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private StackRepository stackRepository;

    public Set<Template> retrievePrivateTemplates(CbUser user) {
        return templateRepository.findForUser(user.getUserId());
    }

    public Set<Template> retrieveAccountTemplates(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return templateRepository.findAllInAccount(user.getAccount());
        } else {
            return templateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Template get(Long id) {
        Template template = templateRepository.findOne(id);
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, id));
        } else {
            return template;
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    public Template create(CbUser user, Template template) {
        LOGGER.debug("Creating template: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Template savedTemplate = null;
        template.setOwner(user.getUserId());
        template.setAccount(user.getAccount());
        try {
            savedTemplate = templateRepository.save(template);
        } catch (Exception ex) {
            throw new DuplicateKeyValueException(APIResourceType.TEMPLATE, template.getName(), ex);
        }
        return savedTemplate;
    }

    public void delete(Long templateId, CbUser user) {
        Template template = templateRepository.findByIdInAccount(templateId, user.getAccount());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateId));
        }
        delete(template, user);
    }

    public Template getPrivateTemplate(String name, CbUser user) {
        Template template = templateRepository.findByNameInUser(name, user.getUserId());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name));
        } else {
            return template;
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Template getPublicTemplate(String name, CbUser user) {
        Template template = templateRepository.findOneByName(name, user.getAccount());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, name));
        } else {
            return template;
        }
    }

    public void delete(String templateName, CbUser user) {
        Template template = templateRepository.findByNameInAccount(templateName, user.getAccount(), user.getUserId());
        if (template == null) {
            throw new NotFoundException(String.format(TEMPLATE_NOT_FOUND_MSG, templateName));
        }
        delete(template, user);
    }

    private void delete(Template template, CbUser user) {
        LOGGER.debug("Deleting template. {} - {}", new Object[]{template.getId(), template.getName()});
        List<Stack> allStackForTemplate = stackRepository.findAllStackForTemplate(template.getId());
        if (allStackForTemplate.isEmpty()) {
            if (!user.getUserId().equals(template.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Templates can be deleted only by account admins or owners.");
            }
            template.setTopology(null);
            if (ResourceStatus.USER_MANAGED.equals(template.getStatus())) {
                templateRepository.delete(template);
            } else {
                template.setStatus(ResourceStatus.DEFAULT_DELETED);
                templateRepository.save(template);
            }
        } else {
            if (isRunningStackReferToTemplate(allStackForTemplate)) {
                throw new BadRequestException(String.format(
                        "There are stacks associated with template '%s'. Please remove these before deleting the template.", template.getName()));
            } else {
                Date now = new Date();
                String terminatedName = template.getName() + DELIMITER + now.getTime();
                template.setName(terminatedName);
                template.setTopology(null);
                template.setDeleted(true);
                if (ResourceStatus.DEFAULT.equals(template.getStatus())) {
                    template.setStatus(ResourceStatus.DEFAULT_DELETED);
                }
                templateRepository.save(template);
            }
        }
    }

    private boolean isRunningStackReferToTemplate(List<Stack> allStackForTemplate) {
        boolean result = false;
        for (Stack stack : allStackForTemplate) {
            if (!stack.isDeleteCompleted()) {
                result = true;
                break;
            }
        }
        return result;
    }

}
