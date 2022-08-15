package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaVerticalScaleServiceTest {

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private FreeIpaVerticalScaleService underTest;

    @Test
    void testUpdateTemplateWithVerticalScaleInformationWithInstanceTypeShouldReturnNewInstanceType() {
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        verticalScaleRequest.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("big1");
        verticalScaleRequest.setTemplate(instanceTemplateRequest);

        Template template = new Template();
        template.setInstanceType("big2");
        template.setId(1L);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName("master");

        when(instanceGroupService.getByStackIdAndInstanceGroupNameWithFetchTemplate(anyLong(), anyString()))
                .thenReturn(Optional.of(instanceGroup));
        when(templateService.findById(anyLong()))
                .thenReturn(Optional.of(template));
        when(templateService.save(any(Template.class)))
                .thenReturn(template);

        underTest.updateTemplateWithVerticalScaleInformation(1L, verticalScaleRequest);

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);

        verify(instanceGroupService, times(1)).getByStackIdAndInstanceGroupNameWithFetchTemplate(anyLong(), anyString());
        verify(templateService, times(1)).findById(any());
        verify(templateService, times(1)).save(captor.capture());
        Assert.assertEquals("big1", captor.getValue().getInstanceType());
    }

    @Test
    void testUpdateTemplateWithVerticalScaleInformationWithDiskShouldReturnNewDiskConfig() {
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        verticalScaleRequest.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        VolumeRequest volumeRequest = new VolumeRequest();
        volumeRequest.setCount(1);
        volumeRequest.setSize(50);
        volumeRequest.setType("ebs");
        instanceTemplateRequest.setAttachedVolumes(Set.of(volumeRequest));
        verticalScaleRequest.setTemplate(instanceTemplateRequest);

        Template template = new Template();
        template.setInstanceType("big2");
        template.setId(1L);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName("master");

        when(instanceGroupService.getByStackIdAndInstanceGroupNameWithFetchTemplate(anyLong(), anyString()))
                .thenReturn(Optional.of(instanceGroup));
        when(templateService.findById(anyLong()))
                .thenReturn(Optional.of(template));
        when(templateService.save(any(Template.class)))
                .thenReturn(template);

        underTest.updateTemplateWithVerticalScaleInformation(1L, verticalScaleRequest);

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);

        verify(instanceGroupService, times(1)).getByStackIdAndInstanceGroupNameWithFetchTemplate(anyLong(), anyString());
        verify(templateService, times(1)).findById(any());
        verify(templateService, times(1)).save(captor.capture());
        Assert.assertEquals(Integer.valueOf(1), captor.getValue().getVolumeCount());
        Assert.assertEquals(Integer.valueOf(50), captor.getValue().getVolumeSize());
        Assert.assertEquals("ebs", captor.getValue().getVolumeType());
    }
}