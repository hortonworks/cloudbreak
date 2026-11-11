package com.sequenceiq.distrox.v1.distrox.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerChangeInstanceTypeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerIncreaseStorageSizeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerChangeInstanceTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerIncreaseStorageSizeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerValidInstanceTypeOptionsV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.service.database.DatabaseServerChangeInstanceTypeService;
import com.sequenceiq.cloudbreak.service.database.DatabaseServerIncreaseStorageSizeService;

@ExtendWith(MockitoExtension.class)
class DistroXDatabaseServerV1ControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:username";

    private static final String ACCOUNT_ID = "accountId";

    private static final String NAME = "datahub";

    private static final String CRN = "crn:cdp:datahub:us-west-1:accountId:cluster:name";

    private static final String INSTANCE_TYPE = "db.m5.xlarge";

    private static final int STORAGE_SIZE_IN_GB = 200;

    @Mock
    private DatabaseServerChangeInstanceTypeService databaseServerChangeInstanceTypeService;

    @Mock
    private DatabaseServerIncreaseStorageSizeService databaseServerIncreaseStorageSizeService;

    @InjectMocks
    private DistroXDatabaseServerV1Controller underTest;

    @Test
    void testChangeDatabaseServerInstanceTypeByName() {
        DatabaseServerChangeInstanceTypeV4Response expected = new DatabaseServerChangeInstanceTypeV4Response();
        when(databaseServerChangeInstanceTypeService.changeInstanceType(NameOrCrn.ofName(NAME), ACCOUNT_ID, INSTANCE_TYPE)).thenReturn(expected);

        DatabaseServerChangeInstanceTypeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.changeDatabaseServerInstanceTypeByName(NAME, changeInstanceTypeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testChangeDatabaseServerInstanceTypeByCrn() {
        DatabaseServerChangeInstanceTypeV4Response expected = new DatabaseServerChangeInstanceTypeV4Response();
        when(databaseServerChangeInstanceTypeService.changeInstanceType(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, INSTANCE_TYPE)).thenReturn(expected);

        DatabaseServerChangeInstanceTypeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.changeDatabaseServerInstanceTypeByCrn(CRN, changeInstanceTypeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testIncreaseDatabaseServerStorageSizeByName() {
        DatabaseServerIncreaseStorageSizeV4Response expected = new DatabaseServerIncreaseStorageSizeV4Response();
        when(databaseServerIncreaseStorageSizeService.increaseStorageSize(NameOrCrn.ofName(NAME), ACCOUNT_ID, STORAGE_SIZE_IN_GB)).thenReturn(expected);

        DatabaseServerIncreaseStorageSizeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.increaseDatabaseServerStorageSizeByName(NAME, increaseStorageSizeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testIncreaseDatabaseServerStorageSizeByCrn() {
        DatabaseServerIncreaseStorageSizeV4Response expected = new DatabaseServerIncreaseStorageSizeV4Response();
        when(databaseServerIncreaseStorageSizeService.increaseStorageSize(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, STORAGE_SIZE_IN_GB)).thenReturn(expected);

        DatabaseServerIncreaseStorageSizeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.increaseDatabaseServerStorageSizeByCrn(CRN, increaseStorageSizeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testGetDatabaseServerValidInstanceTypeOptionsByName() {
        DatabaseServerValidInstanceTypeOptionsV4Response expected = new DatabaseServerValidInstanceTypeOptionsV4Response();
        when(databaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(NameOrCrn.ofName(NAME), ACCOUNT_ID)).thenReturn(expected);

        DatabaseServerValidInstanceTypeOptionsV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getDatabaseServerValidInstanceTypeOptionsByName(NAME));

        assertSame(expected, actual);
    }

    @Test
    void testGetDatabaseServerValidInstanceTypeOptionsByCrn() {
        DatabaseServerValidInstanceTypeOptionsV4Response expected = new DatabaseServerValidInstanceTypeOptionsV4Response();
        when(databaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(NameOrCrn.ofCrn(CRN), ACCOUNT_ID)).thenReturn(expected);

        DatabaseServerValidInstanceTypeOptionsV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getDatabaseServerValidInstanceTypeOptionsByCrn(CRN));

        assertSame(expected, actual);
    }

    private DatabaseServerChangeInstanceTypeV4Request changeInstanceTypeRequest() {
        DatabaseServerChangeInstanceTypeV4Request request = new DatabaseServerChangeInstanceTypeV4Request();
        request.setInstanceType(INSTANCE_TYPE);
        return request;
    }

    private DatabaseServerIncreaseStorageSizeV4Request increaseStorageSizeRequest() {
        DatabaseServerIncreaseStorageSizeV4Request request = new DatabaseServerIncreaseStorageSizeV4Request();
        request.setStorageSizeInGb(STORAGE_SIZE_IN_GB);
        return request;
    }
}
