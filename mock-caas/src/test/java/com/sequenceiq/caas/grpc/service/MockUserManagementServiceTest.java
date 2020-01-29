package com.sequenceiq.caas.grpc.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadPasswordPolicy;
import com.sequenceiq.caas.util.JsonUtil;

import io.grpc.internal.testing.StreamRecorder;

@RunWith(MockitoJUnitRunner.class)
public class MockUserManagementServiceTest {

    private static final String VALID_LICENSE = "License file content";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private MockUserManagementService underTest;

    @Mock
    private JsonUtil jsonUtil;

    @Test
    public void testSetLicenseShouldReturnACloudbreakLicense() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", licenseFilePath.toString());
        underTest.init();

        String actual = ReflectionTestUtils.getField(underTest, "cbLicense").toString();

        Assert.assertEquals(VALID_LICENSE, actual);
        Files.delete(licenseFilePath);
    }

    @Test
    public void testSetLicenseShouldEmptyStringWhenTheFileIsNotExists() {
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", "/etc/license");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The license file could not be found on path: '/etc/license'. " +
                "Please place your CM license file in your '<cbd_path>/etc' folder. By default the name of the file should be 'license.txt'.");

        underTest.init();
    }

    @Test
    public void testCreateWorkloadUsername() {
        String username = "&*foO$_#Bar22@baz13.com";
        String expected = "foo_bar22";

        Assert.assertEquals(expected, underTest.sanitizeWorkloadUsername(username));
    }

    @Test
    public void testGetWorkloadCredentials() {
        underTest.initializeActorWorkloadCredentials();

        long currentTime = System.currentTimeMillis();

        GetActorWorkloadCredentialsRequest req = GetActorWorkloadCredentialsRequest.getDefaultInstance();
        StreamRecorder<GetActorWorkloadCredentialsResponse> observer = StreamRecorder.create();
        underTest.getActorWorkloadCredentials(req, observer);
        Assert.assertEquals(1, observer.getValues().size());
        GetActorWorkloadCredentialsResponse res = observer.getValues().get(0);
        Assert.assertNotNull(res);
        Assert.assertNotNull(res.getPasswordHash());
        Assert.assertNotNull(res.getKerberosKeysList());
        Assert.assertEquals(2, res.getKerberosKeysList().size());
        Assert.assertTrue(res.getPasswordHashExpirationDate() > currentTime);
    }

    @Test
    public void testGetAccountIncludesPasswordPolicy() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", licenseFilePath.toString());
        ReflectionTestUtils.setField(underTest, "accountUsers", Map.of("test", Set.of("user1", "user2")));
        underTest.init();

        GetAccountRequest req = GetAccountRequest.newBuilder().setAccountId("test").build();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();
        underTest.getAccount(req, observer);
        Assert.assertEquals(1, observer.getValues().size());
        GetAccountResponse res = observer.getValues().get(0);
        Assert.assertTrue(res.hasAccount());
        Account account = res.getAccount();
        Assert.assertTrue(account.hasPasswordPolicy());
        WorkloadPasswordPolicy passwordPolicy = account.getPasswordPolicy();
        Assert.assertEquals(MockUserManagementService.PASSWORD_LIFETIME, passwordPolicy.getWorkloadPasswordMaxLifetime());
    }

    @Test
    public void testGetAccountForInvalidAccount() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", licenseFilePath.toString());
        ReflectionTestUtils.setField(underTest, "accountUsers", Map.of("test", Set.of("user1", "user2")));
        underTest.init();

        GetAccountRequest req = GetAccountRequest.newBuilder().setAccountId("invalidAccount").build();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();
        try {
            underTest.getAccount(req, observer);
        } catch (io.grpc.StatusRuntimeException e) {
            Assert.assertEquals("UNKNOWN", e.getStatus().getCode().name());
        }
    }
}