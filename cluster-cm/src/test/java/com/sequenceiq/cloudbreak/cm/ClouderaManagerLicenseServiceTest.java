package com.sequenceiq.cloudbreak.cm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerLicenseServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String LICENSE = "license";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private ClouderaManagerLicenseService underTest;

    @Test
    public void validWhenLicenseIsNotEmpty() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey(LICENSE).build();
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID), any())).thenReturn(account);

        underTest.validateClouderaManagerLicense(ACCOUNT_ID);

        verify(umsClient).getAccountDetails(eq(ACCOUNT_ID), any());
    }

    @Test
    public void invalidWhenLicenseIsEmpty() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey("").build();
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID), any())).thenReturn(account);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("For this tenant there is no valid cloudera manager license.");

        underTest.validateClouderaManagerLicense(ACCOUNT_ID);

        verify(umsClient).getAccountDetails(eq(ACCOUNT_ID), any());
    }

}