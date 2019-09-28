package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class TemplateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateService.class);

    private static final String TEMPLATE_NOT_FOUND_MSG = "Template '%s' not found.";

    private static final String TEMPLATE_NOT_FOUND_BY_ID_MSG = "Template not found by id '%d'.";

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private StackService stackService;

    public Set<Template> retrievePrivateTemplates(CloudbreakUser user) {
        return templateRepository.findForUser(user.getUserId());
    }

    public Set<Template> retrieveAccountTemplates(CloudbreakUser user) {
        return templateRepository.findForUser(user.getUserId(), user.getAccount());
    }

    public Template get(Long id) {
        return templateRepository.findById(id).orElseThrow(notFound("Template", id));
    }

    public Template create(String owner, String account, User user, Template template, Workspace workspace) {
        LOGGER.debug("Creating template: [User: '{}']", user.getUserId());

        template.setOwner(owner);
        template.setAccount(account);
        template.setWorkspace(workspace);

        Template savedTemplate;
        try {
            savedTemplate = templateRepository.save(template);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedTemplate;
    }

    public void delete(Long templateId, CloudbreakUser user) {
        Template template = Optional.ofNullable(templateRepository.findByIdInAccount(templateId, user.getAccount()))
                .orElseThrow(notFound("Template", templateId));
        delete(template);
    }

    public Template getPrivateTemplate(String name, CloudbreakUser user) {
        return Optional.ofNullable(templateRepository.findByNameInUser(name, user.getUserId()))
                .orElseThrow(notFound("Template", name));
    }

    public Template getPublicTemplate(String name, CloudbreakUser user) {
        return Optional.ofNullable(templateRepository.findOneByName(name, user.getAccount()))
                .orElseThrow(notFound("Template", name));
    }

    public void delete(String templateName, CloudbreakUser user) {
        Template template = Optional.ofNullable(templateRepository.findByNameInAccount(templateName, user.getAccount(), user.getUserId()))
                .orElseThrow(notFound("Template", templateName));
        delete(template);
    }

    public void delete(Template template) {
        LOGGER.info("Deleting template. {} - {}", new Object[]{template.getId(), template.getName()});
        if (!stackService.templateInUse(template.getId())) {
            template.setTopology(null);
            if (ResourceStatus.USER_MANAGED.equals(template.getStatus())) {
                templateRepository.delete(template);
            } else {
                template.setName(NameUtil.postfixWithTimestamp(template.getName()));
                template.setStatus(ResourceStatus.DEFAULT_DELETED);
                template.setDeleted(true);
                templateRepository.save(template);
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with template '%s'. Please remove these before deleting the template.", template.getName()));
        }
    }

    public Set<Template> findByTopology(Topology topology) {
        return templateRepository.findByTopology(topology);
    }
}
