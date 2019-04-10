package com.sequenceiq.cloudbreak.auth.altus;

import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class GrpcUmsClientTest {

    @Mock
    private UmsConfig umsConfigMock;

    @InjectMocks
    private GrpcUmsClient testedClass = new GrpcUmsClient();

    @Test
    @Ignore
    public void getUserDetails() {

        when(umsConfigMock.getEndpoint()).thenReturn("ums.thunderhead-dev.cloudera.com");
        when(umsConfigMock.getPort()).thenReturn(8982);

        String exampleCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

        UserManagementProto.User user = testedClass.getUserDetails(exampleCrn, exampleCrn, Optional.of("uuid"));
        user.getCrn();
    }
}