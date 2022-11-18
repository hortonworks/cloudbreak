package com.sequenceiq.cloudbreak.auth.altus.config;

import static org.glassfish.jersey.internal.guava.Preconditions.checkArgument;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

import javax.annotation.PostConstruct;

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

    @Value("${altus.ums.client.list_workload_administration_groups_for_member_page_size:100}")
    private int listWorkloadAdministrationGroupsForMemberPageSize;

    @Value("${altus.ums.client.list_service_principal_cloud_identities_page_size:100}")
    private int listServicePrincipalCloudIdentitiesPageSize;

    @Value("${altus.ums.caller:cloudbreak}")
    private String callingServiceName;

    @Value("${altus.ums.client.grpc.timeout.sec:60}")
    private long grpcTimeoutSec;

    @Value("${altus.ums.client.grpc.short.timeout.sec:5}")
    private long grpcShortTimeoutSec;

    @PostConstruct
    public void init() {
        checkNotNull(callingServiceName, "callingServiceName must not be null.");
        checkArgument(!callingServiceName.isBlank(), "callingServiceName must not be blank.");
    }

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

    public int getListWorkloadAdministrationGroupsForMemberPageSize() {
        return listWorkloadAdministrationGroupsForMemberPageSize;
    }

    public int getListServicePrincipalCloudIdentitiesPageSize() {
        return listServicePrincipalCloudIdentitiesPageSize;
    }

    public long getGrpcTimeoutSec() {
        return grpcTimeoutSec;
    }

    public String getCallingServiceName() {
        return callingServiceName;
    }

    public long getGrpcShortTimeoutSec() {
        return grpcShortTimeoutSec;
    }

}
