package com.sequenceiq.it.cloudbreak.dto.diagnostics;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

public abstract class BaseCMDiagnosticsTestDto<R extends BaseCmDiagnosticsCollectionRequest,
        D extends BaseCMDiagnosticsTestDto,
        C extends MicroserviceClient> extends AbstractTestDto<R, FlowIdentifier, D, C> {

    private final Class<C> clientTypeClass;

    public BaseCMDiagnosticsTestDto(String newId, Class<C> clientTypeClass) {
        super(newId);
        this.clientTypeClass = clientTypeClass;
    }

    public BaseCMDiagnosticsTestDto(R request, TestContext testContext, Class<C> clientTypeClass) {
        super(request, testContext);
        this.clientTypeClass = clientTypeClass;
    }

    public D withDefaults() {
        withDestination(DiagnosticsDestination.CLOUD_STORAGE);
        withSkipValidation(true);
        // set this to false for no network test case !
        withUpdatePackage(true);
        return (D) this;
    }

    public D withDestination(DiagnosticsDestination destination) {
        getRequest().setDestination(destination);
        return (D) this;
    }

    public D withUpdatePackage(boolean updatePackage) {
        getRequest().setUpdatePackage(updatePackage);
        return (D) this;
    }

    public D withSkipValidation(boolean skipValidation) {
        getRequest().setSkipValidation(skipValidation);
        return (D) this;
    }

    @Override
    public D when(Class<D> entityClass, Action<D, C> action) {
        return getTestContext().when(entityClass, clientTypeClass, action, emptyRunningParameter());
    }

    @Override
    public D when(Action<D, C> action) {
        return getTestContext().when((D) this, clientTypeClass, action, emptyRunningParameter());
    }

    @Override
    public D when(Class<D> entityClass, Action<D, C> action, RunningParameter runningParameter) {
        return getTestContext().when(entityClass, clientTypeClass, action, runningParameter);
    }

    @Override
    public D when(Action<D, C> action, RunningParameter runningParameter) {
        return getTestContext().when((D) this, clientTypeClass, action, runningParameter);
    }

    @Override
    public D then(Assertion<D, C> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public D then(Assertion<D, C> assertion, RunningParameter runningParameter) {
        return getTestContext().then((D) this, clientTypeClass, assertion, runningParameter);
    }

    @Override
    public D then(List<Assertion<D, C>> assertions) {
        List<RunningParameter> runningParameters = new ArrayList<>(assertions.size());
        for (int i = 0; i < assertions.size(); i++) {
            runningParameters.add(emptyRunningParameter());
        }
        return then(assertions, runningParameters);
    }

    @Override
    public D then(List<Assertion<D, C>> assertions, List<RunningParameter> runningParameters) {
        for (int i = 0; i < assertions.size() - 1; i++) {
            getTestContext().then((D) this, clientTypeClass, assertions.get(i), runningParameters.get(i));
        }
        return getTestContext()
                .then((D) this, clientTypeClass, assertions.get(assertions.size() - 1), runningParameters.get(runningParameters.size() - 1));
    }

    @Override
    public <T extends CloudbreakTestDto> T deleteGiven(Class<T> clazz, Action<T, C> action, RunningParameter runningParameter) {
        getTestContext().when((T) getTestContext().given(clazz), clientTypeClass, action, runningParameter);
        return getTestContext().expect((T) getTestContext().given(clazz), BadRequestException.class, runningParameter);
    }

    @Override
    public void validate() {
        throw new NotImplementedException(String.format("The entity(%s) must be implement the valid() method.", getClass()));
    }
}
