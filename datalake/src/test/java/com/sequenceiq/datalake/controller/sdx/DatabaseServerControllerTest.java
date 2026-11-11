package com.sequenceiq.datalake.controller.sdx;

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
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseServerChangeInstanceTypeService;
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseServerIncreaseStorageSizeService;

@ExtendWith(MockitoExtension.class)
class DatabaseServerControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:username";

    private static final String NAME = "datalake";

    private static final String CRN = "crn:cdp:datalake:us-west-1:accountId:datalake:name";

    private static final String INSTANCE_TYPE = "db.m5.xlarge";

    private static final int STORAGE_SIZE_IN_GB = 200;

    @Mock
    private SdxDatabaseServerChangeInstanceTypeService sdxDatabaseServerChangeInstanceTypeService;

    @Mock
    private SdxDatabaseServerIncreaseStorageSizeService sdxDatabaseServerIncreaseStorageSizeService;

    @InjectMocks
    private DatabaseServerController underTest;

    @Test
    void testChangeDatabaseServerInstanceTypeByName() {
        DatabaseServerChangeInstanceTypeV4Response expected = new DatabaseServerChangeInstanceTypeV4Response();
        when(sdxDatabaseServerChangeInstanceTypeService.changeInstanceType(USER_CRN, NameOrCrn.ofName(NAME), INSTANCE_TYPE)).thenReturn(expected);

        DatabaseServerChangeInstanceTypeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.changeDatabaseServerInstanceTypeByName(NAME, changeInstanceTypeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testChangeDatabaseServerInstanceTypeByCrn() {
        DatabaseServerChangeInstanceTypeV4Response expected = new DatabaseServerChangeInstanceTypeV4Response();
        when(sdxDatabaseServerChangeInstanceTypeService.changeInstanceType(USER_CRN, NameOrCrn.ofCrn(CRN), INSTANCE_TYPE)).thenReturn(expected);

        DatabaseServerChangeInstanceTypeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.changeDatabaseServerInstanceTypeByCrn(CRN, changeInstanceTypeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testIncreaseDatabaseServerStorageSizeByName() {
        DatabaseServerIncreaseStorageSizeV4Response expected = new DatabaseServerIncreaseStorageSizeV4Response();
        when(sdxDatabaseServerIncreaseStorageSizeService.increaseStorageSize(USER_CRN, NameOrCrn.ofName(NAME), STORAGE_SIZE_IN_GB)).thenReturn(expected);

        DatabaseServerIncreaseStorageSizeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.increaseDatabaseServerStorageSizeByName(NAME, increaseStorageSizeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testIncreaseDatabaseServerStorageSizeByCrn() {
        DatabaseServerIncreaseStorageSizeV4Response expected = new DatabaseServerIncreaseStorageSizeV4Response();
        when(sdxDatabaseServerIncreaseStorageSizeService.increaseStorageSize(USER_CRN, NameOrCrn.ofCrn(CRN), STORAGE_SIZE_IN_GB)).thenReturn(expected);

        DatabaseServerIncreaseStorageSizeV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.increaseDatabaseServerStorageSizeByCrn(CRN, increaseStorageSizeRequest()));

        assertSame(expected, actual);
    }

    @Test
    void testGetDatabaseServerValidInstanceTypeOptionsByName() {
        DatabaseServerValidInstanceTypeOptionsV4Response expected = new DatabaseServerValidInstanceTypeOptionsV4Response();
        when(sdxDatabaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(USER_CRN, NameOrCrn.ofName(NAME))).thenReturn(expected);

        DatabaseServerValidInstanceTypeOptionsV4Response actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getDatabaseServerValidInstanceTypeOptionsByName(NAME));

        assertSame(expected, actual);
    }

    @Test
    void testGetDatabaseServerValidInstanceTypeOptionsByCrn() {
        DatabaseServerValidInstanceTypeOptionsV4Response expected = new DatabaseServerValidInstanceTypeOptionsV4Response();
        when(sdxDatabaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(USER_CRN, NameOrCrn.ofCrn(CRN))).thenReturn(expected);

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
