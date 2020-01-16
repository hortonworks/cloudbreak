package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiEcho;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiUser2;
import com.cloudera.api.swagger.model.ApiUser2List;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.AnswerWithoutRequest;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.ClouderaManagerPreparedRequestAnswer;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.StringRequestAnswer;

public final class ClouderaManagerEndpoints {
    public static final String API_ROOT = "/api/v31";

    private ClouderaManagerEndpoints() {
    }

    @SparkUri(url = API_ROOT + "/cm/importClusterTemplate", requestType = ApiClusterTemplate.class)
    public interface ClusterTemplateImport {
        ClouderaManagerPreparedRequestAnswer<ApiCommand, ApiClusterTemplate> post();
    }

    @SparkUri(url = API_ROOT + "/tools/echo")
    public interface Echo {
        AnswerWithoutRequest<ApiEcho> get();
    }

    @SparkUri(url = API_ROOT + "/cm/version")
    public interface Version {
        AnswerWithoutRequest<ApiVersionInfo> get();
    }

    @SparkUri(url = API_ROOT + "/authRoles/metadata")
    public interface AuthRoles {
        AnswerWithoutRequest<ApiAuthRoleMetadataList> get();
    }

    @SparkUri(url = API_ROOT + "/users", requestType = ApiUser2.class)
    public interface Users {
        AnswerWithoutRequest<ApiUser2List> get();

        ClouderaManagerPreparedRequestAnswer<ApiUser2List, ApiUser2> post();

        @SparkUri(url = API_ROOT + "/users/:user")
        ClouderaManagerPreparedRequestAnswer<ApiUser2, ApiUser2> put();
    }

    public interface Commands {
        @SparkUri(url = API_ROOT + "/cm/commands/:command")
        StringRequestAnswer<ApiCommand> post();

        @SparkUri(url = API_ROOT + "/cm/commands")
        StringRequestAnswer<ApiCommandList> get();
    }

    @SparkUri(url = API_ROOT + "/cm/service")
    public interface Service {
        StringRequestAnswer<ApiService> put();

        AnswerWithoutRequest<ApiService> get();

        @SparkUri(url = API_ROOT + "/cm/service/autoConfigure")
        StringRequestAnswer<ApiCommand> putAutoConfigure();
    }

    @SparkUri(url = API_ROOT + "/cm/service/commands")
    public interface ServiceCommands {
        @SparkUri(url = API_ROOT + "/cm/service/commands/:command", requestType = String.class)
        StringRequestAnswer<ApiCommand> post();

        AnswerWithoutRequest<ApiCommandList> get();
    }

    @SparkUri(url = API_ROOT + "/cm/trial/begin", requestType = String.class)
    public interface FreeTrial {
        StringRequestAnswer<ApiCommand> post();
    }

    @SparkUri(url = API_ROOT + "/cm/config", requestType = String.class)
    public interface Config {
        AnswerWithoutRequest<ApiConfigList> get();

        StringRequestAnswer<ApiConfigList> put();
    }

    public interface CdpRemoteContext {
        @SparkUri(url = "/api/cdp/remoteContext/byCluster/:clusterName", requestType = String.class)
        AnswerWithoutRequest<ApiRemoteDataContext> get();

        @SparkUri(url = "/api/cdp/remoteContext", requestType = String.class)
        StringRequestAnswer<ApiRemoteDataContext> post();
    }

    public interface ClouderaManagerHosts {
        @SparkUri(url = API_ROOT + "/hosts")
        AnswerWithoutRequest<ApiHostList> get();

        @SparkUri(url = API_ROOT + "/hosts/:hostId")
        AnswerWithoutRequest<ApiHostList> getById();

        @SparkUri(url = API_ROOT + "/hosts/:hostId")
        AnswerWithoutRequest<ApiHostList> deleteById();
    }

    public interface ClusterCommands {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/commands")
        AnswerWithoutRequest<ApiCommandList> get();

        @SparkUri(url = API_ROOT + "/clusters/:clusterName/commands/:command")
        StringRequestAnswer<ApiCommand> post();
    }

    public interface ClusterServices {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/services")
        AnswerWithoutRequest<ApiServiceList> get();
    }

    public interface ClusterHosts {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/hosts")
        AnswerWithoutRequest<ApiServiceList> get();

        @SparkUri(url = API_ROOT + "/clusters/:clusterName/hosts/:hostId")
        AnswerWithoutRequest<ApiServiceList> delete();
    }

    public interface ClusterHostTemplates {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/hostTemplates")
        AnswerWithoutRequest<ApiHostTemplateList> get();
    }

    public interface ClusterServiceRoles {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/services/:serviceName/roles")
        AnswerWithoutRequest<ApiRoleList> get();

        @SparkUri(url = API_ROOT + "/clusters/:clusterName/services/:serviceName/roles/:roleName")
        AnswerWithoutRequest<ApiRole> delete();
    }

    //API_ROOT + "/clusters/:clusterName/parcels"
    public interface ClusterParcel {
        @SparkUri(url = API_ROOT + "/clusters/:clusterName/parcels")
        AnswerWithoutRequest<ApiParcelList> get();
    }
}