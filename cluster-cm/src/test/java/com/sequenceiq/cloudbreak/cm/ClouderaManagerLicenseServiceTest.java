package com.sequenceiq.cloudbreak.cm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.workspace.model.User;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerLicenseServiceTest {

    private static final String USER_CRN = "userCrn";

    private static final String LICENSE = "license";

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private ClouderaManagerLicenseService underTest;

    @Test
    public void testBeginTrialIfNeededShouldNotBeginTrialWhenLicenseKeyIsPresent() throws ApiException {
        User user = createUser();
        ApiClient apiClient = mock(ApiClient.class);
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey(LICENSE).build();
        when(umsClient.getAccountDetails(USER_CRN, USER_CRN, Optional.empty())).thenReturn(account);

        underTest.beginTrialIfNeeded(user, apiClient);

        verify(umsClient).getAccountDetails(USER_CRN, USER_CRN, Optional.empty());
        verifyZeroInteractions(clouderaManagerClientFactory);
    }

    @Test
    public void testBeginTrialIfNeededShouldBeginTrialWhenLicenseKeyIsNotPresent() throws ApiException {
        User user = createUser();
        ApiClient apiClient = mock(ApiClient.class);
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder().setClouderaManagerLicenseKey("").build();
        ClouderaManagerResourceApi resourceApi = mock(ClouderaManagerResourceApi.class);

        when(umsClient.getAccountDetails(USER_CRN, USER_CRN, Optional.empty())).thenReturn(account);
        when(clouderaManagerClientFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(resourceApi);

        underTest.beginTrialIfNeeded(user, apiClient);

        verify(umsClient).getAccountDetails(USER_CRN, USER_CRN, Optional.empty());
        verify(clouderaManagerClientFactory).getClouderaManagerResourceApi(apiClient);
    }

    private User createUser() {
        User user = new User();
        user.setUserCrn(USER_CRN);
        return user;
    }

}