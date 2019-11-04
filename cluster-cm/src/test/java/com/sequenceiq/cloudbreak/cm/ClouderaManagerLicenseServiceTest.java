package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
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
import com.sequenceiq.cloudbreak.workspace.model.User;

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
        User user = createUser();
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey(LICENSE).build();
        when(umsClient.getAccountDetails(USER_CRN, ACCOUNT_ID, Optional.empty())).thenReturn(account);

        underTest.validateClouderaManagerLicense(user);

        verify(umsClient).getAccountDetails(USER_CRN, ACCOUNT_ID, Optional.empty());
    }

    @Test
    public void invalidWhenLicenseIsEmpty() {
        User user = createUser();
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey("").build();
        when(umsClient.getAccountDetails(USER_CRN, ACCOUNT_ID, Optional.empty())).thenReturn(account);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("User doesn't have a valid cloudera manager license.");

        underTest.validateClouderaManagerLicense(user);

        verify(umsClient).getAccountDetails(USER_CRN, ACCOUNT_ID, Optional.empty());
    }

    private User createUser() {
        User user = new User();
        user.setUserCrn(USER_CRN);
        return user;
    }

}