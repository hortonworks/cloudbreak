package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public final class ClouderaManagerEndpoints<T extends CloudbreakTestDto> {
    public static final String API_ROOT = "/{mockUuid}/api/v31";

    public static final String API_ROOT_V40 = "/{mockUuid}/api/v40";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public ClouderaManagerEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public Users<T> users() {
        return (Users<T>) EndpointProxyFactory.create(Users.class, testDto, mockedTestContext);
    }

    public Admin<T> usersAdmin() {
        return (Admin<T>) EndpointProxyFactory.create(Admin.class, testDto, mockedTestContext);
    }

    public ClusterHosts<T> clusterHosts() {
        return (ClusterHosts<T>) EndpointProxyFactory.create(ClusterHosts.class, testDto, mockedTestContext);
    }

    public ClusterCommands.DeployConfig<T> clusterCommandsDeployConfig() {
        return (ClusterCommands.DeployConfig<T>) EndpointProxyFactory.create(ClusterCommands.DeployConfig.class, testDto, mockedTestContext);
    }

    public ClusterHostTemplates.CommandsApplyHostTemplate<T> commandsApplyHostTemplate() {
        return (ClusterHostTemplates.CommandsApplyHostTemplate<T>)
                EndpointProxyFactory.create(ClusterHostTemplates.CommandsApplyHostTemplate.class, testDto, mockedTestContext);
    }

    public Commands<T> commands() {
        return (Commands<T>) EndpointProxyFactory.create(Commands.class, testDto, mockedTestContext);
    }

    public ClouderaManagerHosts<T> clouderaManagerHosts() {
        return (ClouderaManagerHosts<T>) EndpointProxyFactory.create(ClouderaManagerHosts.class, testDto, mockedTestContext);
    }

    public ExternalUserMappings<T> externalUserMappings() {
        return (ExternalUserMappings<T>) EndpointProxyFactory.create(ExternalUserMappings.class, testDto, mockedTestContext);
    }

    public ClusterTemplateImport<T> importClusterTemplate() {
        return (ClusterTemplateImport<T>) EndpointProxyFactory.create(ClusterTemplateImport.class, testDto, mockedTestContext);
    }

    public T profile(String profile, int times) {
        mockedTestContext.getExecuteQueryToMockInfrastructure().execute(testDto.getCrn() + "/profile/" + profile + "/" + times, r -> r);
        return testDto;
    }

    @SparkUri(url = API_ROOT_V40 + "/cm/importClusterTemplate", requestType = ApiClusterTemplate.class)
    public interface ClusterTemplateImport<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

        @Override
        DefaultResponseConfigure<T, ApiCommand> post();
    }

    @SparkUri(url = API_ROOT + "/externalUserMappings")
    public interface ExternalUserMappings<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiExternalUserMappingList> post();
    }

    @SparkUri(url = API_ROOT + "/tools/echo")
    public interface Echo<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiEcho> get();
    }

    @SparkUri(url = API_ROOT + "/cm/version")
    public interface Version<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiVersionInfo> get();
    }

    @SparkUri(url = API_ROOT + "/authRoles/metadata")
    public interface AuthRoles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiAuthRoleMetadataList> get();
    }

    @SparkUri(url = API_ROOT + "/users", requestType = ApiUser2.class)
    public interface Users<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, Void> get();

        DefaultResponseConfigure<T, ApiUser2List> post();

        @SparkUri(url = API_ROOT + "/users/{user}")
        DefaultResponseConfigure<T, ApiUser2> put();
    }

    public interface Admin<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/users/admin")
        DefaultResponseConfigure<T, ApiUser2> put();
    }

    public interface CMCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/cm/commands/{command}")
        DefaultResponseConfigure<T, ApiCommand> post();

        @SparkUri(url = API_ROOT + "/cm/commands")
        DefaultResponseConfigure<T, ApiCommand> get();
    }

    public interface Commands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/commands/{commandId}")
        DefaultResponseConfigure<T, ApiCommand> getById();
    }

    @SparkUri(url = API_ROOT + "/cm/service")
    public interface Service<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiService> put();

        DefaultResponseConfigure<T, ApiService> get();

        @SparkUri(url = API_ROOT + "/cm/service/autoConfigure")
        DefaultResponseConfigure<T, Void> putAutoConfigure();
    }

    @SparkUri(url = API_ROOT + "/cm/service/commands")
    public interface ServiceCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/cm/service/commands/{command}")
        DefaultResponseConfigure<T, ApiCommand> post();

        DefaultResponseConfigure<T, ApiCommandList> get();
    }

    @SparkUri(url = API_ROOT + "/cm/config")
    public interface Config<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiConfigList> get();

        DefaultResponseConfigure<T, ApiConfigList> put();
    }

    public interface ClouderaManagerHosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/hosts")
        DefaultResponseConfigure<T, ApiHostList> get();
    }

    public interface ClusterCommands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands")
        DefaultResponseConfigure<T, ApiCommandList> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands/{command}")
        DefaultResponseConfigure<T, ApiCommand> post();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/commands/deployClientConfig")
        interface DeployConfig<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, ApiCommand> post();
        }
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services")
    public interface ClusterServices<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiServiceList> get();
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hosts")
    public interface ClusterHosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiHostRefList> get();

        DefaultResponseConfigure<T, ApiHostRefList> post();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hosts/{hostId}")
        DefaultResponseConfigure<T, ApiHostRef> delete();
    }

    @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hostTemplates")
    public interface ClusterHostTemplates<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        DefaultResponseConfigure<T, ApiHostTemplateList> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/hostTemplates/{hostTemplateName}/commands/applyHostTemplate")
        interface CommandsApplyHostTemplate<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, ApiCommand> post();
        }
    }

    public interface ClusterServiceRoles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services/{serviceName}/roles")
        DefaultResponseConfigure<T, ApiRoleList> get();

        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/services/{serviceName}/roles/{roleName}")
        DefaultResponseConfigure<T, ApiRole> delete();
    }

    //API_ROOT + "/clusters/{clusterName}/parcels"
    public interface ClusterParcel<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
        @SparkUri(url = API_ROOT + "/clusters/{clusterName}/parcels")
        DefaultResponseConfigure<T, ApiParcelList> get();
    }
}