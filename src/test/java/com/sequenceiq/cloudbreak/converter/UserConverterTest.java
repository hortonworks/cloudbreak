package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anySetOf;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.AccountRepository;

public class UserConverterTest {

    private static final String DUMMY_PASSWORD = "DUMMY_PASSWORD";
    private static final String DUMMY_LAST_NAME = "Gipsz";
    private static final String DUMMY_FIRST_NAME = "Jakab";
    private static final String DUMMY_EMAIL = "gipszjakab@myemail.com";
    private static final String DUMMY_COMPANY = "SequenceIQ";

    @InjectMocks
    private UserConverter underTest;

    @Mock
    private AzureTemplateConverter azureTemplateConverter;

    @Mock
    private AwsTemplateConverter awsTemplateConverter;

    @Mock
    private StackConverter stackConverter;

    @Mock
    private BlueprintConverter blueprintConverter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountConverter accountConverter;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AwsCredentialConverter awsCredentialConverter;

    @Mock
    private AzureCredentialConverter azureCredentialConverter;

    private User user;

    private UserJson userJson;

    @Before
    public void setUp() {
        underTest = new UserConverter();
        MockitoAnnotations.initMocks(this);
//        user = createUser();
        userJson = createUserJson();
    }

    @Test
    public void testConvertUserEntityToJson() {
        // GIVEN
        given(awsTemplateConverter.convertAllEntityToJson(anySetOf(AwsTemplate.class)))
                .willReturn(new HashSet<TemplateJson>());
        given(azureTemplateConverter.convertAllEntityToJson(anySetOf(AzureTemplate.class)))
                .willReturn(new HashSet<TemplateJson>());
        given(stackConverter.convertAllEntityToJson(anySetOf(Stack.class)))
                .willReturn(new HashSet<StackJson>());
        given(blueprintConverter.convertAllEntityToJson(anySetOf(Blueprint.class)))
                .willReturn(new HashSet<BlueprintJson>());
        // WHEN
        UserJson result = underTest.convert(user);
        // THEN
        assertEquals(result.getCompany(), user.getAccount().getName());
        assertEquals(result.getFirstName(), user.getFirstName());
        assertNotNull(result.getAwsTemplates());
        assertNotNull(result.getAzureTemplates());
        assertNotNull(result.getCredentials());
        assertNotNull(result.getStacks());
        assertNotNull(result.getBlueprints());
    }

//    @Test
//    public void testConvertUserJsonToEntity() {
//        // GIVEN
//        given(awsTemplateConverter.convertAllJsonToEntity(anySetOf(TemplateJson.class)))
//                .willReturn(new HashSet<AwsTemplate>());
//        given(azureTemplateConverter.convertAllJsonToEntity(anySetOf(TemplateJson.class)))
//                .willReturn(new HashSet<AzureTemplate>());
//        given(stackConverter.convertAllJsonToEntity(anySetOf(StackJson.class)))
//                .willReturn(new HashSet<Stack>());
//        given(passwordEncoder.encode(anyString())).willReturn(DUMMY_PASSWORD);
//
//        given(accountConverter.convert(any(AccountJson.class))).willReturn(createAccount());
//
//        // WHEN
//        User result = underTest.convert(userJson);
//        // THEN
//        assertEquals(result.getFirstName(), userJson.getFirstName());
//        assertEquals(result.getStacks().size(), userJson.getStacks().size());
//        assertNotNull(result.getAwsCredentials());
//        assertNotNull(result.getAwsTemplates());
//        assertNotNull(result.getAzureTemplates());
//        assertNotNull(result.getAzureCredentials());
//        assertNotNull(result.getClusters());
//        assertNotNull(result.getStacks());
//        assertNotNull(result.getBlueprints());
//        verify(passwordEncoder, times(1)).encode(anyString());
//    }
//
//    private User createUser() {
//        User user = new User();
//        user.setAwsCredentials(new HashSet<AwsCredential>());
//        user.setAwsTemplates(new HashSet<AwsTemplate>());
//        user.setAzureCredentials(new HashSet<AzureCredential>());
//        user.setAzureTemplates(new HashSet<AzureTemplate>());
//        user.setBlueprints(new HashSet<Blueprint>());
//        user.setClusters(new HashSet<Cluster>());
//        user.setAccount(createAccount());
//        user.setConfToken(null);
//        user.setEmail(DUMMY_EMAIL);
//        user.setFirstName(DUMMY_FIRST_NAME);
//        user.setLastName(DUMMY_LAST_NAME);
//        user.setId(1L);
//        user.setLastLogin(new Date());
//        user.setPassword(DUMMY_PASSWORD);
//        user.setRegistrationDate(new Date());
//        user.setStacks(new HashSet<Stack>());
//        user.setStatus(UserStatus.ACTIVE);
//        return user;
//    }

    private UserJson createUserJson() {
        UserJson userJson = new UserJson();
        userJson.setAwsTemplates(new HashSet<TemplateJson>());
        userJson.setAzureTemplates(new HashSet<TemplateJson>());
        userJson.setBlueprints(new HashSet<BlueprintJson>());
        userJson.setCredentials(new HashSet<CredentialJson>());
        userJson.setEmail(DUMMY_EMAIL);
        userJson.setFirstName(DUMMY_FIRST_NAME);
        userJson.setLastName(DUMMY_LAST_NAME);
        userJson.setPassword(DUMMY_PASSWORD);
        userJson.setStacks(new HashSet<StackJson>());
        userJson.setCompany("SequenceIQ");
        return userJson;
    }

    private Account createAccount() {
        Account account = new Account();
        account.setName("SequenceIQ");
        return account;
    }
}
