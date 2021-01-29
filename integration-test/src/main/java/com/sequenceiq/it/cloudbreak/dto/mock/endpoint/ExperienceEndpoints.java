package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import java.util.function.Consumer;

import javax.ws.rs.core.Response;

import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.MockUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;

public final class ExperienceEndpoints<T extends CloudbreakTestDto> {

    public static final String LIFTIE_API_ROOT = "/liftie/api/v1";

    public static final String DWX_API_ROOT = "/dwx";

    private T testDto;

    private MockedTestContext mockedTestContext;

    public ExperienceEndpoints(T testDto, MockedTestContext mockedTestContext) {
        this.testDto = testDto;
        this.mockedTestContext = mockedTestContext;
    }

    public Liftie.Cluster.ByClusterId<T> liftieExperience() {
        return (Liftie.Cluster.ByClusterId<T>) EndpointProxyFactory.create(Liftie.Cluster.ByClusterId.class, testDto, mockedTestContext);
    }

    public Dwx.ByCrn<T> dwxExperience() {
        return (Dwx.ByCrn<T>) EndpointProxyFactory.create(Dwx.ByCrn.class, testDto, mockedTestContext);
    }

    public Liftie.Cluster<T> listLiftieExperience() {
        return (Liftie.Cluster<T>) EndpointProxyFactory.create(Liftie.Cluster.class, testDto, mockedTestContext);
    }

    public T mockCreateLiftieExperience(String env, Consumer<Response> consumer) {
        EndpointProxyFactory.create(Liftie.Mocksupport.ByEnvironmentId.class, testDto, mockedTestContext).post()
                .pathVariable("environmentId", env).crnless().execute(consumer, null);
        return testDto;
    }

    public T mockCreateDwxExperience(String crn, Consumer<Response> consumer) {
        EndpointProxyFactory.create(Dwx.Mocksupport.ByCrn.class, testDto, mockedTestContext).post()
                .pathVariable("crn", crn).crnless().execute(consumer, null);
        return testDto;
    }


    @MockUri(url = LIFTIE_API_ROOT)
    public interface Liftie {

        interface Cluster<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

            interface ByClusterId<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

                DefaultResponseConfigure<T, Object> delete();
            }

            DefaultResponseConfigure<T, Object> get();
        }

        interface Mocksupport<T extends CloudbreakTestDto> {

            interface ByEnvironmentId<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, Object> post();
            }

            interface Experience<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, Object> put();

                DefaultResponseConfigure<T, Object> post();

            }
        }
    }

    @MockUri(url = DWX_API_ROOT)
    public interface Dwx {

        interface ByCrn<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {

            DefaultResponseConfigure<T, Object> delete();

            DefaultResponseConfigure<T, Object> get();
        }

        interface Mocksupport<T extends CloudbreakTestDto> {

            interface ByCrn<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                DefaultResponseConfigure<T, Object> post();
            }

            interface Experience<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                interface ById<T extends CloudbreakTestDto> extends VerificationEndpoint<T> {
                    DefaultResponseConfigure<T, Object> put();

                    DefaultResponseConfigure<T, Object> post();
                }
            }
        }
    }
}