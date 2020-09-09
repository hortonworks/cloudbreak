package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.services.sqladmin.SQLAdmin;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerCheckerService;

@Service
public class GcpDatabaseServerCheckService extends GcpDatabaseServerBaseService implements DatabaseServerCheckerService {

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext ac, List<CloudResource> resources, ResourceStatus waitedStatus) {
        SQLAdmin sqlAdmin = GcpStackUtil.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
        return checkResources(resourceType(), sqlAdmin, ac, resources, waitedStatus);
    }
}
