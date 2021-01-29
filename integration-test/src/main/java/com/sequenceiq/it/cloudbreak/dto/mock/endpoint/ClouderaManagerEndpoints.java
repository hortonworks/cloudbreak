package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiUser2;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

public final class ClouderaManagerEndpoints<T extends CloudbreakTestDto> {
    public static final String API_ROOT = "/api/v31";

    public static final String ACTIVE_COMMANDS = "/cmf/commands/activeCommandTable";

    public static final String RECENT_COMMANDS = "/cmf/commands/commandTable";

    private T testDto;

    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public ClouderaManagerEndpoints(T testDto, ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure) {
        this.testDto = testDto;
        this.executeQueryToMockInfrastructure = executeQueryToMockInfrastructure;
    }

    public Users<T> users() {
        return (Users<T>) EndpointProxyFactory.create(Users.class, testDto, executeQueryToMockInfrastructure);
    }

    public Admin<T> usersAdmin() {
        return (Admin<T>) EndpointProxyFactory.create(Admin.class, testDto, executeQueryToMockInfrastructure);
    }

    public ClusterHosts<T> clusterHosts() {
        return (ClusterHosts<T>) EndpointProxyFactory.create(ClusterHosts.class, testDto, executeQueryToMockInfrastructure);
    }

    public ClusterCommands.DeployConfig<T> clusterCommandsDeployConfig() {
        return (ClusterCommands.DeployConfig<T>) EndpointProxyFactory.create(ClusterCommands.DeployConfig.class, testDto, executeQueryToMockInfrastructure);
    }

    public ClusterHostTemplates.CommandsApplyHostTemplate<T> commandsApplyHostTemplate() {
        return (ClusterHostTemplates.CommandsApplyHostTemplate<T>)
                EndpointProxyFactory.create(ClusterHostTemplates.CommandsApplyHostTemplate.class, testDto, executeQueryToMockInfrastructure);
    }

    public Commands<T> commands() {
        return (Commands<T>) EndpointProxyFactory.create(Commands.class, testDto, executeQueryToMockInfrastructure);
    }

    public ClouderaManagerHosts<T> clouderaManagerHosts() {
        return (ClouderaManagerHosts<T>) EndpointProxyFactory.create(ClouderaManagerHosts.class, testDto, executeQueryToMockInfrastructure);
    }

    public ActiveCommandTable<T> cmActiveCommands() {
        return (ActiveCommandTable<T>) EndpointProxyFactory.create(
                ActiveCommandTable.class, testDto, executeQueryToMockInfrastructure);
    }

    public RecentCommandTable<T> cmRecentCommands() {
        return (RecentCommandTable<T>) EndpointProxyFactory.create(
                RecentCommandTable.class, testDto, executeQueryToMockInfrastructure);
    }

    @SparkUri(url = ACTIVE_COMMANDS)
    public interface ActiveCommandTable<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = RECENT_COMMANDS)
    public interface RecentCommandTable<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/cm/importClusterTemplate", requestType = ApiClusterTemplate.class)
    public interface ClusterTemplateImport<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

        @Override
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = API_ROOT + "/tools/echo")
    public interface Echo<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/cm/version")
    public interface Version<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/authRoles/metadata")
    public interface AuthRoles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/users", requestType = ApiUser2.class)
    public interface Users<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();

        DefaultResponseConfigure<T> post();

        @SparkUri(url = API_ROOT + "/users/{user}")
        DefaultResponseConfigure<T> put();
    }

    public interface Admin<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/api/v31/users/admin")
        DefaultResponseConfigure<T> put();
    }

    public interface CMCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/cm/commands/{command}")
        DefaultResponseConfigure<T> post();

        @SparkUri(url = API_ROOT + "/cm/commands")
        DefaultResponseConfigure<T> get();
    }

    public interface Commands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/commands/{commandId}")
        DefaultResponseConfigure<T> getById();
    }

    @SparkUri(url = API_ROOT + "/cm/service")
    public interface Service<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> put();

        DefaultResponseConfigure<T> get();

        @SparkUri(url = API_ROOT + "/cm/service/autoConfigure")
        DefaultResponseConfigure<T> putAutoConfigure();
    }

    @SparkUri(url = API_ROOT + "/cm/service/commands")
    public interface ServiceCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/cm/service/commands/{command}")
        DefaultResponseConfigure<T> post();

        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/cm/trial/begin")
    public interface FreeTrial<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> post();
    }

    @SparkUri(url = API_ROOT + "/cm/config")
    public interface Config<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();

        DefaultResponseConfigure<T> put();
    }

    public interface CdpRemoteContext<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = "/api/cdp/remoteContext/byCluster/{clusterName}")
        DefaultResponseConfigure<T> get();

        @SparkUri(url = "/api/cdp/remoteContext")
        DefaultResponseConfigure<T> post();
    }

    public interface ClouderaManagerHosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/hosts")
        DefaultResponseConfigure<T> get();

        @SparkUri(url = API_ROOT + "/hosts/{hostId}")
        DefaultResponseConfigure<T> getById();

        @SparkUri(url = API_ROOT + "/hosts/{hostId}")
        DefaultResponseConfigure<T> deleteById();
    }

    public interface ClusterCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands")
        DefaultResponseConfigure<T> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands/{command}")
        DefaultResponseConfigure<T> post();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands/deployClientConfig")
        interface DeployConfig<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T> post();
        }
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services")
    public interface ClusterServices<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hosts")
    public interface ClusterHosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();

        DefaultResponseConfigure<T> post();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hosts/{hostId}")
        DefaultResponseConfigure<T> delete();
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hostTemplates")
    public interface ClusterHostTemplates<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hostTemplates/{hostTemplateName}/commands/applyHostTemplate")
        interface CommandsApplyHostTemplate<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T> post();
        }
    }

    public interface ClusterServiceRoles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services/{serviceName}/roles")
        DefaultResponseConfigure<T> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services/{serviceName}/roles/{roleName}")
        DefaultResponseConfigure<T> delete();
    }

    //API_ROOT + "/clusters/{clusterName}/parcels"
    public interface ClusterParcel<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/parcels")
        DefaultResponseConfigure<T> get();
    }
}