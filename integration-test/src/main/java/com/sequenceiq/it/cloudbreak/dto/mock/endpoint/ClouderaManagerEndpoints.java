package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.cloudera.api.swagger.model.ApiAuthRoleMetadataList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiExternalUserMappingList;
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
import com.sequenceiq.it.cloudbreak.dto.mock.MockUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ClouderaManagerEndpoints.CmV31Api.ClustersByClusterName.HostTemplates.ByHostTemplateName.CommandsApplyHostTemplate;

public final class ClouderaManagerEndpoints<T extends CloudbreakTestDto> {
    public static final String API_ROOT = "/{mockUuid}/api/v31";

    public static final String API_ROOT_V40 = "/{mockUuid}/api/v40";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public ClouderaManagerEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public CmV31Api.Users<T> users() {
        return (CmV31Api.Users<T>) EndpointProxyFactory.create(CmV31Api.Users.class, testDto, mockedTestContext);
    }

    public CmV31Api.Hosts<T> hosts() {
        return (CmV31Api.Hosts<T>) EndpointProxyFactory.create(CmV31Api.Hosts.class, testDto, mockedTestContext);
    }

    public CmV31Api.ExternalUserMappings<T> externalUserMappings() {
        return (CmV31Api.ExternalUserMappings<T>) EndpointProxyFactory.create(CmV31Api.ExternalUserMappings.class, testDto, mockedTestContext);
    }

    public Cm40Api.Cm.ImportClusterTemplate<T> cmImportClusterTemplate() {
        return (Cm40Api.Cm.ImportClusterTemplate<T>) EndpointProxyFactory.create(Cm40Api.Cm.ImportClusterTemplate.class, testDto, mockedTestContext);
    }

    public CmV31Api.ClustersByClusterName.Hosts<T> clustersByClusterNameHost() {
        return (CmV31Api.ClustersByClusterName.Hosts<T>) EndpointProxyFactory.create(CmV31Api.ClustersByClusterName.Hosts.class, testDto, mockedTestContext);
    }

    public CommandsApplyHostTemplate<T> clustersByClusterNameHostTemplatesByHostTemplateNameCommandsApplyHostTemplate() {
        return (CommandsApplyHostTemplate<T>)
                EndpointProxyFactory.create(CommandsApplyHostTemplate.class, testDto, mockedTestContext);
    }

    public T profile(String profile, int times) {
        mockedTestContext.getExecuteQueryToMockInfrastructure().execute(testDto.getCrn() + "/profile/" + profile + "/" + times, r -> r);
        return testDto;
    }

    @MockUri(url = API_ROOT_V40)
    public interface Cm40Api {
        interface Cm {

            interface ImportClusterTemplate<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiCommand> post();
            }
        }
    }

    @MockUri(url = API_ROOT)
    public interface CmV31Api {

        interface ExternalUserMappings<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, ApiExternalUserMappingList> get();
        }

        interface Tools {
            interface Echo<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiVersionInfo> get();
            }
        }

        interface AuthRoles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            interface Metadata<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiAuthRoleMetadataList> get();
            }
        }

        interface Users<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, Void> get();

            DefaultResponseConfigure<T, ApiUser2List> post();

            interface ByUser<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiUser2> put();
            }
        }

        interface Cm {

            interface Version<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiVersionInfo> get();
            }

            interface Commands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiCommand> get();

                interface ByCommand<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                    DefaultResponseConfigure<T, ApiCommand> post();
                }
            }

            interface Config<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiConfigList> get();
            }

            interface Service<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, ApiService> put();

                DefaultResponseConfigure<T, ApiService> get();

                interface AutoConfigure<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                    DefaultResponseConfigure<T, Void> put();
                }

                interface Commands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                    DefaultResponseConfigure<T, ApiCommand> post();

                    interface ByCommand<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                        DefaultResponseConfigure<T, ApiCommandList> get();
                    }
                }
            }
        }

        interface Hosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
            DefaultResponseConfigure<T, ApiServiceList> get();
        }

        interface ClustersByClusterName {

            interface Hosts<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, ApiHostRefList> get();

                DefaultResponseConfigure<T, ApiHostRefList> post();

                interface ByHostId<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                    DefaultResponseConfigure<T, ApiHostRef> delete();
                }
            }

            interface HostTemplates<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, ApiHostTemplateList> get();

                interface ByHostTemplateName {

                    @MockUri(url = "/commands/applyHostTemplate")
                    interface CommandsApplyHostTemplate<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                        DefaultResponseConfigure<T, ApiCommand> delete();
                    }
                }
            }

            interface Services<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, ApiServiceList> get();

                interface ByServiceName {
                    interface Roles<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                        DefaultResponseConfigure<T, ApiRoleList> get();

                        interface ByRoleName<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                            DefaultResponseConfigure<T, ApiRole> delete();
                        }
                    }
                }
            }

            interface Commands<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, ApiCommand> get();

                interface ByCommand<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                    DefaultResponseConfigure<T, ApiCommand> post();
                }
            }

            interface Parcels<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, ApiParcelList> get();
            }
        }
    }
}