package com.sequenceiq.cloudbreak.auth.altus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UmsClientConfig {
    @Value("${altus.ums.client.list_groups_page_size:100}")
    private int listGroupsPageSize;

    @Value("${altus.ums.client.list_users_page_size:100}")
    private int listUsersPageSize;

    @Value("${altus.ums.client.list_machine_users_page_size:100}")
    private int listMachineUsersPageSize;

    @Value("${altus.ums.client.list_workload_administration_groups_page_size:100}")
    private int listWorkloadAdministrationGroupsPageSize;

    public int getListGroupsPageSize() {
        return listGroupsPageSize;
    }

    public int getListUsersPageSize() {
        return listUsersPageSize;
    }

    public int getListMachineUsersPageSize() {
        return listMachineUsersPageSize;
    }

    public int getListWorkloadAdministrationGroupsPageSize() {
        return listWorkloadAdministrationGroupsPageSize;
    }
}
