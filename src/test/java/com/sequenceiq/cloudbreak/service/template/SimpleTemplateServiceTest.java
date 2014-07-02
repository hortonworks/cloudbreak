package com.sequenceiq.cloudbreak.service.template;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.AwsTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SimpleTemplateServiceTest {

    @InjectMocks
    private SimpleTemplateService underTest;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private AwsTemplateConverter awsTemplateConverter;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private TemplateJson templateJson;

    private User user;

    private AwsTemplate awsTemplate;

    @Before
    public void setUp() {
        underTest = new SimpleTemplateService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        awsTemplate = new AwsTemplate();
        awsTemplate.setId(1L);
        awsTemplate.setUser(user);
    }

    @Test
    public void testCreateAwsTemplate() {
        //GIVEN
        given(awsTemplateConverter.convert(templateJson)).willReturn(awsTemplate);
        given(templateJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
        given(templateRepository.save(awsTemplate)).willReturn(awsTemplate);
        //WHEN
        underTest.create(user, templateJson);
        //THEN
        verify(templateRepository, times(1)).save(awsTemplate);
    }

    @Test
    public void testDeleteAwsTemplate() {
        //GIVEN
        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
        given(stackRepository.findAllStackForTemplate(1L)).willReturn(new ArrayList<Stack>());
        doNothing().when(templateRepository).delete(awsTemplate);
        //WHEN
        underTest.delete(1L);
        //THEN
        verify(templateRepository, times(1)).delete(awsTemplate);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteAwsTemplateWhenTemplateNotFound() {
        //GIVEN
        given(templateRepository.findOne(1L)).willReturn(null);
        //WHEN
        underTest.delete(1L);
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteAwsTemplateWhenStackListIsEmpty() {
        //GIVEN
        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
        given(stackRepository.findAllStackForTemplate(1L)).willReturn(Arrays.asList(new Stack()));
        doNothing().when(templateRepository).delete(awsTemplate);
        //WHEN
        underTest.delete(1L);
    }
}
