package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.User;
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

    public Template get(Long id) {
        return templateRepository.findById(id).orElseThrow(notFound("Template", id));
    }

    public Template create(User user, Template template) {
        LOGGER.debug("Creating template: [User: '{}']", user.getUserId());

        try {
            return templateRepository.save(template);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
    }

    public Template savePure(Template template) {
        return templateRepository.save(template);
    }

    public void delete(Template template) {
        LOGGER.debug("Deleting template. {} - {}", new Object[]{template.getId(), template.getName()});
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
