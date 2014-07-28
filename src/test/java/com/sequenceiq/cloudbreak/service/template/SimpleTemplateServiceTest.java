package com.sequenceiq.cloudbreak.service.template;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.AwsTemplateConverter;
import com.sequenceiq.cloudbreak.converter.AzureTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySetOf;
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
    private AzureTemplateConverter azureTemplateConverter;

    @Mock
    private AzureCertificateService azureCertificateService;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private TemplateJson templateJson;

    private User user;

    private AwsTemplate awsTemplate;

    private AzureTemplate azureTemplate;

    @Before
    public void setUp() {
        underTest = new SimpleTemplateService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        awsTemplate = new AwsTemplate();
        awsTemplate.setId(1L);
        awsTemplate.setUser(user);
        azureTemplate = new AzureTemplate();
        azureTemplate.setId(1L);
        azureTemplate.setUser(user);
    }

    @Test
    public void testCreateAwsTemplate() {
        //GIVEN
        given(awsTemplateConverter.convert(templateJson)).willReturn(awsTemplate);
        given(templateJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
        given(templateRepository.save(awsTemplate)).willReturn(awsTemplate);
        //doNothing().when(historyService).notify(any(ProvisionEntity.class), any(HistoryEvent.class));
        //WHEN
        underTest.create(user, templateJson);
        //THEN
        verify(templateRepository, times(1)).save(awsTemplate);
    }

    @Test
    public void testCreateAzureTemplate() {
        // GIVEN
        Map<String, Object> params = new HashMap<>();
        given(azureTemplateConverter.convert(templateJson)).willReturn(azureTemplate);
        given(templateJson.getParameters()).willReturn(params);
        given(templateJson.getCloudPlatform()).willReturn(CloudPlatform.AZURE);
        given(templateRepository.save(azureTemplate)).willReturn(azureTemplate);
        // WHEN
        underTest.create(user, templateJson);
        // THEN
    }

    @Test
    public void testDeleteTemplate() {
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
    public void testDeleteTemplateWhenTemplateNotFound() {
        //GIVEN
        given(templateRepository.findOne(1L)).willReturn(null);
        //WHEN
        underTest.delete(1L);
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteTemplateWhenStackListIsEmpty() {
        //GIVEN
        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
        given(stackRepository.findAllStackForTemplate(1L)).willReturn(Arrays.asList(new Stack()));
        doNothing().when(templateRepository).delete(awsTemplate);
        //WHEN
        underTest.delete(1L);
    }

    @Test
    public void testGetAll() {
        // GIVEN
        given(awsTemplateConverter.convertAllEntityToJson(anySetOf(AwsTemplate.class))).willReturn(new HashSet<TemplateJson>());
        given(azureTemplateConverter.convertAllEntityToJson(anySetOf(AzureTemplate.class))).willReturn(new HashSet<TemplateJson>());
        // WHEN
        underTest.getAll(user);
        // THEN
        verify(awsTemplateConverter, times(1)).convertAllEntityToJson(anySetOf(AwsTemplate.class));
        verify(azureTemplateConverter, times(1)).convertAllEntityToJson(anySetOf(AzureTemplate.class));
    }

    @Test
    public void testGetAwsTemplate() {
        // GIVEN
        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
        given(awsTemplateConverter.convert(any(AwsTemplate.class))).willReturn(templateJson);
        // WHEN
        underTest.get(1L);
        // THEN
        verify(awsTemplateConverter, times(1)).convert(any(AwsTemplate.class));
    }

    @Test
    public void testGetAzureTemplate() {
        // GIVEN
        given(templateRepository.findOne(1L)).willReturn(azureTemplate);
        given(azureTemplateConverter.convert(any(AzureTemplate.class))).willReturn(templateJson);
        // WHEN
        underTest.get(1L);
        // THEN
        verify(azureTemplateConverter, times(1)).convert(any(AzureTemplate.class));
    }
}
