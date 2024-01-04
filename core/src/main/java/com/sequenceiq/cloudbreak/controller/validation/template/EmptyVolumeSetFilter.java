package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class EmptyVolumeSetFilter {

    @Inject
    private TemplateService templateService;

    public Template filterOutVolumeSetsWhichAreEmpty(Template template) {
        if (template.getVolumeTemplates() != null) {
            Set<VolumeTemplate> filteredVolumeTemplates = template.getVolumeTemplates()
                    .stream()
                    .filter(e -> e.getVolumeCount() != 0 && e.getVolumeSize() != 0)
                    .collect(Collectors.toSet());
            template.setVolumeTemplates(filteredVolumeTemplates);
        }
        return templateService.savePure(template);
    }
}
