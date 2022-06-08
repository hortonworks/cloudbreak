package com.sequenceiq.cloudbreak.service.customconfigs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.CustomConfigurationsCreationException;
import com.sequenceiq.cloudbreak.repository.CustomConfigurationPropertyRepository;
import com.sequenceiq.cloudbreak.repository.CustomConfigurationsRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.CustomConfigurationsValidator;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsServiceTest {

    private static final String TEST_NAME = "test";

    private static final String TEST_VERSION = "7.2.10";

    private static final String TEST_ACCOUNT_ID_1 = "accid";

    private static final String TEST_USER_CRN_1 = "crn:cdp:iam:us-west-1:" + TEST_ACCOUNT_ID_1 + ":user:username";

    private static final String TEST_CRN_1 = "crn:cdp:resource:us-west-1:" + TEST_ACCOUNT_ID_1 + ":customconfigurations:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    private final Set<CustomConfigurationProperty> configs = Set.of(
            // service-wide config
            new CustomConfigurationProperty("property1", "value1", null, "service1"),
            // role-specific configs
            new CustomConfigurationProperty("property2", "value2", "role2", "service1"),
            new CustomConfigurationProperty("property3", "value3", "role3", "service2")
    );

    private CustomConfigurations customConfigurations = new CustomConfigurations(TEST_NAME,
            TEST_CRN_1, configs, TEST_VERSION, TEST_ACCOUNT_ID_1, null);

    @Mock
    private CustomConfigurationsRepository customConfigurationsRepository;

    @Mock
    private CustomConfigurationPropertyRepository customConfigurationPropertyRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private SecretService secretService;

    @Mock
    private CustomConfigurationsValidator validator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Captor
    private ArgumentCaptor<Set<CustomConfigurationProperty>> argumentCaptor;

    @InjectMocks
    private CustomConfigurationsService underTest;

    @Test
    void testIfCustomConfigsAreRetrievedByCrn() {
        when(customConfigurationsRepository.findByCrn(TEST_CRN_1)).thenReturn(Optional.of(customConfigurations));

        CustomConfigurations returnedValue = underTest.getByNameOrCrn(NameOrCrn.ofCrn(TEST_CRN_1));

        assertEquals(customConfigurations, returnedValue);
        verify(customConfigurationsRepository).findByCrn(TEST_CRN_1);
    }

    @Test
    void testIfCustomConfigsAreRetrievedByName() {
        when(customConfigurationsRepository.findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID_1)).thenReturn(Optional.of(customConfigurations));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN_1, () -> {
            CustomConfigurations returnedValue = underTest.getByNameOrCrn(NameOrCrn.ofName(TEST_NAME));
            assertEquals(customConfigurations, returnedValue);
            assertThrows(NotFoundException.class, () -> underTest.getByNameOrCrn(NameOrCrn.ofName("not a valid name")));
            verify(customConfigurationsRepository).findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID_1);
        });
    }

    @Test
    void testIfCustomConfigsAreBeingAddedCorrectly() throws TransactionService.TransactionExecutionException {
        doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString(), anyString());
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN_1, () -> underTest.create(customConfigurations, TEST_ACCOUNT_ID_1));

        verify(customConfigurationPropertyRepository).saveAll(argumentCaptor.capture());
        Set<CustomConfigurationProperty> capturedValue = argumentCaptor.getValue();
        Set<CustomConfigurationProperty> expectedValue = customConfigurations.getConfigurations();
        assertEquals(expectedValue, capturedValue);
        assertEquals(customConfigurations, new ArrayList<>(expectedValue).get(0).getCustomConfigs());
    }

    @Test
    void testThrowsExceptionWhenNotDeletedClusterHasCustomConfigs() {
        when(clusterService.findByCustomConfigurations(customConfigurations)).thenReturn(Set.of(mock(Cluster.class)));
        when(customConfigurationsRepository.findByCrn(anyString())).thenReturn(Optional.of(customConfigurations));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.deleteByCrn(TEST_CRN_1));

        assertEquals(String.format("There is a cluster associated with the Custom Configurations '%s'. " +
                "Please delete the cluster before deleting the Custom Configurations. " +
                "The following cluster is associated with these Custom Configurations: %s", TEST_NAME, null), actual.getMessage());
    }

    @Test
    void testDuplicateNameForSameAccountThrowsException() {
        when(customConfigurationsRepository.findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID_1)).thenReturn(Optional.of(customConfigurations));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN_1,
                () -> assertThrows(CustomConfigurationsCreationException.class, () -> underTest.create(customConfigurations, TEST_ACCOUNT_ID_1)));
    }

    @Test
    void testCustomConfigurationsAreDeletedByName() {
        when(customConfigurationsRepository.findByNameAndAccountId(TEST_NAME, TEST_ACCOUNT_ID_1)).thenReturn(Optional.of(customConfigurations));
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN_1, () -> {
            CustomConfigurations result = underTest.deleteByName(TEST_NAME, TEST_ACCOUNT_ID_1);
            verify(customConfigurationPropertyRepository, times(1)).deleteAll(result.getConfigurations());
        });
    }

    @Test
    void testCustomConfigurationsAreDeletedByCrn() {
        when(customConfigurationsRepository.findByCrn(TEST_CRN_1)).thenReturn(Optional.of(customConfigurations));
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN_1, () -> {
            CustomConfigurations result = underTest.deleteByCrn(TEST_CRN_1);
            verify(customConfigurationPropertyRepository, times(1)).deleteAll(result.getConfigurations());
        });
    }
}