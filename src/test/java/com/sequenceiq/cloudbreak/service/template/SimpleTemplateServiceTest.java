package com.sequenceiq.cloudbreak.service.template;

import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.converter.AwsTemplateConverter;
import com.sequenceiq.cloudbreak.converter.AzureTemplateConverter;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;

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

    private AwsTemplate awsTemplate;

    private AzureTemplate azureTemplate;

    @Before
    public void setUp() {
        underTest = new SimpleTemplateService();
        MockitoAnnotations.initMocks(this);
        awsTemplate = new AwsTemplate();
        awsTemplate.setId(1L);
        azureTemplate = new AzureTemplate();
        azureTemplate.setId(1L);
    }

//    @Test
//    public void testCreateAwsTemplate() {
//        // GIVEN
//        given(templateJson.getCloudPlatform()).willReturn(CloudPlatform.AWS);
//        given(templateRepository.save(awsTemplate)).willReturn(awsTemplate);
//        // doNothing().when(historyService).notify(any(ProvisionEntity.class),
//        // any(HistoryEvent.class));
//        // WHEN
//        underTest.create(user, awsTemplate);
//        // THEN
//        verify(templateRepository, times(1)).save(awsTemplate);
//    }
//
//    @Test
//    public void testCreateAzureTemplate() {
//        // GIVEN
//        Map<String, Object> params = new HashMap<>();
//        given(azureTemplateConverter.convert(templateJson)).willReturn(azureTemplate);
//        given(templateJson.getParameters()).willReturn(params);
//        given(templateJson.getCloudPlatform()).willReturn(CloudPlatform.AZURE);
//        given(templateRepository.save(azureTemplate)).willReturn(azureTemplate);
//        // WHEN
//        underTest.create(user, azureTemplate);
//        // THEN
//    }
//
//    @Test
//    public void testDeleteTemplate() {
//        // GIVEN
//        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
//        given(stackRepository.findAllStackForTemplate(1L)).willReturn(new ArrayList<Stack>());
//        doNothing().when(templateRepository).delete(awsTemplate);
//        // WHEN
//        underTest.delete(1L);
//        // THEN
//        verify(templateRepository, times(1)).delete(awsTemplate);
//    }
//
//    @Test(expected = NotFoundException.class)
//    public void testDeleteTemplateWhenTemplateNotFound() {
//        // GIVEN
//        given(templateRepository.findOne(1L)).willReturn(null);
//        // WHEN
//        underTest.delete(1L);
//    }
//
//    @Test(expected = BadRequestException.class)
//    public void testDeleteTemplateWhenStackListIsEmpty() {
//        // GIVEN
//        given(templateRepository.findOne(1L)).willReturn(awsTemplate);
//        given(stackRepository.findAllStackForTemplate(1L)).willReturn(Arrays.asList(new Stack()));
//        doNothing().when(templateRepository).delete(awsTemplate);
//        // WHEN
//        underTest.delete(1L);
//    }
//
//    @Test
//    public void testGetAllForAccountAdminWithAccountUserWithTemplates() {
//        // GIVEN
//        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
//        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
//        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
//        // admin has credentials
//        admin.getAwsTemplates().add((AwsTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AWS, UserRole.ACCOUNT_ADMIN));
//        admin.getAzureTemplates().add((AzureTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AZURE, UserRole.ACCOUNT_ADMIN));
//        // cUser also has credentials
//        cUser.getAwsTemplates().add((AwsTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AWS, UserRole.ACCOUNT_USER));
//        given(accountService.accountUsers(account.getId())).willReturn(new HashSet<User>(Arrays.asList(cUser)));
//
//        // WHEN
//        Set<Template> templates = underTest.getAll(admin);
//
//        // THEN
//        Assert.assertNotNull(templates);
//        Assert.assertTrue("The number of the returned blueprints is right", templates.size() == 3);
//    }
//
//    @Test
//    public void testGetAllForAccountUserWithVisibleAccountTemplates() {
//
//        // GIVEN
//        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
//        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
//        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
//        // admin has credentials
//        admin.getAwsTemplates().add((AwsTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AWS, UserRole.ACCOUNT_ADMIN));
//        admin.getAzureTemplates().add((AzureTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AZURE, UserRole.ACCOUNT_USER));
//        // cUser also has credentials
//        cUser.getAwsTemplates().add((AwsTemplate) ServiceTestUtils.createTemplate(admin, CloudPlatform.AWS, UserRole.ACCOUNT_USER));
//        given(accountService.accountUserData(account.getId(), UserRole.ACCOUNT_USER)).willReturn(admin);
//
//        // WHEN
//        Set<Template> templates = underTest.getAll(cUser);
//
//        // THEN
//        Assert.assertNotNull(templates);
//        Assert.assertTrue("The number of the returned blueprints is right", templates.size() == 3);
//
//    }

}
