package com.sequenceiq.cloudbreak.controller.validation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@ExtendWith(MockitoExtension.class)
class EmptyVolumeSetFilterTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private EmptyVolumeSetFilter emptyVolumeSetFilter;

    @Test
    void testFilterOutVolumeSetsWhichAreEmpty() {
        Template template = new Template();
        Set<VolumeTemplate> volumeTemplates = new HashSet<>();
        volumeTemplates.add(volumeTemplate(1, 10));
        volumeTemplates.add(volumeTemplate(2, 0));
        volumeTemplates.add(volumeTemplate(0, 20));
        template.setVolumeTemplates(volumeTemplates);

        when(templateService.savePure(any(Template.class))).thenReturn(template);

        Template result = emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(template);

        assertFalse(result.getVolumeTemplates().stream().anyMatch(e -> e.getVolumeCount() == 0 || e.getVolumeSize() == 0));
        assertEquals(1, result.getVolumeTemplates().size());
        verify(templateService, times(1)).savePure(result);
    }

    private VolumeTemplate volumeTemplate(int count, int size) {
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeSize(size);
        volumeTemplate.setVolumeCount(count);
        return volumeTemplate;
    }
}