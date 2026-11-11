package com.sequenceiq.cloudbreak.service.database;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerChangeInstanceTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerValidInstanceTypeOptionsV4Response;

@Service
public class DatabaseServerChangeInstanceTypeService {

    // TODO: replace the stubs with the change instance type flow trigger and the redbeams lookups
    public DatabaseServerChangeInstanceTypeV4Response changeInstanceType(NameOrCrn nameOrCrn, String accountId, String instanceType) {
        throw new UnsupportedOperationException("Changing the instance type of the database server of a Data Hub is not implemented yet");
    }

    public DatabaseServerValidInstanceTypeOptionsV4Response getValidInstanceTypeOptions(NameOrCrn nameOrCrn, String accountId) {
        throw new UnsupportedOperationException("Listing the valid instance types of the database server of a Data Hub is not implemented yet");
    }
}
