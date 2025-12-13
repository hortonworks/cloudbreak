package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerLicenseServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String LICENSE = "license";

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private ClouderaManagerLicenseService underTest;

    @Test
    void validWhenLicenseIsNotEmpty() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey(LICENSE).build();
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID))).thenReturn(account);

        underTest.validateClouderaManagerLicense(ACCOUNT_ID);

        verify(umsClient).getAccountDetails(eq(ACCOUNT_ID));
    }

    @Test
    void invalidWhenLicenseIsEmpty() {
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey("").build();
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID))).thenReturn(account);

        assertThrows(RuntimeException.class, () -> underTest.validateClouderaManagerLicense(ACCOUNT_ID),
                "For this tenant there is no valid cloudera manager license.");

        verify(umsClient).getAccountDetails(eq(ACCOUNT_ID));
    }

}