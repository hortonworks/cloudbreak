package com.sequenceiq.caas.grpc.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.caas.util.JsonUtil;

import io.grpc.stub.StreamObserver;

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
    public void testgetActorWorkloadCredentials() {
        GetActorWorkloadCredentialsRequest req = GetActorWorkloadCredentialsRequest.getDefaultInstance();
        GetActorWorkloadCredentialsResponse expected;
        final int keyType17 = 17;
        final int keyType18 = 18;
        final int saltType = 4;

        GetActorWorkloadCredentialsResponse.Builder respBuilder = GetActorWorkloadCredentialsResponse.getDefaultInstance().toBuilder();
        respBuilder.addKerberosKeysBuilder(0)
            .setSaltType(saltType)
            .setKeyType(keyType17)
            .setKeyValue("testKeyValue17")
            .setSaltValue("NonIodizedGrainOfSalt")
            .build();

        respBuilder.addKerberosKeysBuilder(1)
            .setSaltType(saltType)
            .setKeyType(keyType18)
            .setKeyValue("testKeyValue18")
            .setSaltValue("IodizedGrainOfSalt")
            .build();

        respBuilder.setPasswordHash("015353916be1489289e59166124bbcf21c78595f3717f71b079c469c513e05e7");
        expected = respBuilder.build();

        MockUMSServiceCallStreamObserverImpl<GetActorWorkloadCredentialsRequest, GetActorWorkloadCredentialsResponse> responseObserver
            = new MockUMSServiceCallStreamObserverImpl();


        underTest.getActorWorkloadCredentials(req, responseObserver);
        GetActorWorkloadCredentialsResponse actualResp = responseObserver.getResponse();
        Assert.assertEquals(expected, actualResp);
    }

    private static class MockUMSServiceCallStreamObserverImpl<T, R> implements StreamObserver {
        R response;

        @Override
        public void onNext(Object value) {
            response = (R) value;
        }

        @Override
        public void onError(Throwable t) {
            throw new RuntimeException(t);

        }

        @Override
        public void onCompleted() {
            if (response == null) {
                throw new RuntimeException("Response is not being set properly");
            }
        }

        public R getResponse() {
            if (response == null) {
                throw new RuntimeException("Response is not being set properly");
            }
            return response;
        }
    }

}