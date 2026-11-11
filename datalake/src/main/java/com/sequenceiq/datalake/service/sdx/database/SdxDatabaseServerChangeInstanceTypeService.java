package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerChangeInstanceTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerValidInstanceTypeOptionsV4Response;

@Service
public class SdxDatabaseServerChangeInstanceTypeService {

    // TODO: replace the stubs with the change instance type flow trigger and the redbeams lookups
    public DatabaseServerChangeInstanceTypeV4Response changeInstanceType(String userCrn, NameOrCrn nameOrCrn, String instanceType) {
        throw new UnsupportedOperationException("Changing the instance type of the database server of a Data Lake is not implemented yet");
    }

    public DatabaseServerValidInstanceTypeOptionsV4Response getValidInstanceTypeOptions(String userCrn, NameOrCrn nameOrCrn) {
        throw new UnsupportedOperationException("Listing the valid instance types of the database server of a Data Lake is not implemented yet");
    }
}
