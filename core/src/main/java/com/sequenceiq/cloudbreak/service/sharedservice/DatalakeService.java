package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.HierarchyAuthResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class DatalakeService implements HierarchyAuthResourcePropertyProvider {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    private static final String DEFAULT_RANGER_PORT = "6080";

    private static final int FIRST = 0;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    public void prepareDatalakeRequest(Stack source, StackV4Request stackRequest) {
        if (!Strings.isNullOrEmpty(source.getDatalakeCrn())) {
            LOGGER.debug("Prepare datalake request by datalakecrn");
            SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
            Optional<ResourceBasicView> datalakeStack = stackService.getResourceBasicViewByResourceCrn(source.getDatalakeCrn());
            datalakeStack.ifPresent(s -> {
                sharedServiceRequest.setDatalakeName(s.getName());
            });
            stackRequest.setSharedService(sharedServiceRequest);
        }
    }

    public void addSharedServiceResponse(ClusterApiView cluster, ClusterViewV4Response clusterResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (cluster.getStack().getDatalakeCrn() != null) {
            LOGGER.debug("Add shared service response by datalakeCrn");
            Optional<ResourceBasicView> datalakeStack = stackService.getResourceBasicViewByResourceCrn(cluster.getStack().getDatalakeCrn());
            datalakeStack.ifPresent(s -> {
                sharedServiceResponse.setSharedClusterId(s.getId());
                sharedServiceResponse.setSharedClusterName(s.getName());
            });
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    public void addSharedServiceResponse(String datalakeCrn, StackV4Response stackResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (!Strings.isNullOrEmpty(datalakeCrn)) {
            LOGGER.debug("Checking datalake through the datalakeCrn.");
            Optional<ResourceBasicView> resourceBasicView = stackService.getResourceBasicViewByResourceCrn(datalakeCrn);
            if (resourceBasicView.isPresent()) {
                ResourceBasicView s = resourceBasicView.get();
                sharedServiceResponse.setSharedClusterId(s.getId());
                sharedServiceResponse.setSharedClusterName(s.getName());
            } else {
                LOGGER.debug("Unable to find datalake with CRN {}", datalakeCrn);
            }
        }
        stackResponse.setSharedService(sharedServiceResponse);
    }

    public Optional<Stack> getDatalakeStackByDatahubStack(StackView datahubStack) {
        if (!Strings.isNullOrEmpty(datahubStack.getDatalakeCrn())) {
            return Optional.ofNullable(stackService.getByCrnOrElseNull(datahubStack.getDatalakeCrn()));
        }
        LOGGER.info("There is no datalake has been set for the cluster.");
        return Optional.empty();
    }

    public Optional<Stack> getDatalakeStackByStackEnvironmentCrn(StackView stackView) {
        if (StackType.DATALAKE.equals(stackView.getType())) {
            return Optional.empty();
        }
        List<StackIdView> res = stackService.getByEnvironmentCrnAndStackType(stackView.getEnvironmentCrn(), StackType.DATALAKE);
        if (res.isEmpty()) {
            return Optional.empty();
        } else {
            // it has the assumption that environment and datalake has 1:1 connection
            return Optional.of(stackService.getByIdWithListsInTransaction(res.get(FIRST).getId()));
        }
    }

    public String getDatalakeCrn(StackV4Request source, Workspace workspace) {
        if (source.getSharedService() != null && isNotBlank(source.getSharedService().getDatalakeName())) {
            Optional<Stack> result = stackService
                    .findStackByNameOrCrnAndWorkspaceId(NameOrCrn.ofName(source.getSharedService().getDatalakeName()), workspace.getId());
            if (result.isPresent()) {
                return result.get().getResourceCrn();
            } else {
                LOGGER.debug("No datalake resource found for data lake: {}", source.getSharedService().getDatalakeName());
            }
        }
        return null;
    }

    public SharedServiceConfigsView createSharedServiceConfigsView(String password, StackType stackType, String datalakeCrn) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();

        switch (stackType) {
            case DATALAKE:
                setRangerAttributes(password, sharedServiceConfigsView);
                sharedServiceConfigsView.setDatalakeCluster(true);
                break;
            case WORKLOAD:
                sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
                sharedServiceConfigsView.setDatalakeCluster(false);
                sharedServiceConfigsView.setAttachedCluster(true);

                if (Strings.isNullOrEmpty(datalakeCrn)) {
                    break;
                }
                Stack datalakeStack = stackService.getByCrnOrElseNull(datalakeCrn);
                if (datalakeStack == null) {
                    break;
                }

                sharedServiceConfigsView.setDatalakeClusterManagerFqdn(getDatalakeClusterManagerFqdn(datalakeStack));
                sharedServiceConfigsView.setDatalakeClusterManagerIp(datalakeStack.getClusterManagerIp());
                break;
            default:
                setRangerAttributes(password, sharedServiceConfigsView);
                sharedServiceConfigsView.setDatalakeCluster(false);
                break;
        }

        return sharedServiceConfigsView;
    }

    private void setRangerAttributes(String ambariPassword, SharedServiceConfigsView sharedServiceConfigsView) {
        sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
        sharedServiceConfigsView.setAttachedCluster(false);
        sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
    }

    private String getDatalakeClusterManagerFqdn(Stack datalakeStack) {
        String fqdn;
        try {
            fqdn = transactionService.required(() -> datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().isEmpty()
                    ? datalakeStack.getClusterManagerIp()
                    : datalakeStack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN());
        } catch (Exception ignored) {
            LOGGER.debug("Get instance metadata transaction failed, giving back IP as FQDN");
            fqdn = datalakeStack.getClusterManagerIp();
        }
        return fqdn;
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATALAKE;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        String accountId = restRequestThreadLocalService.getAccountId();
        return getNotTerminatedDatalakeStackViewSafely(() -> stackDtoService.findNotTerminatedByNameAndAccountId(resourceName, accountId),
                "%s stack not found", "%s stack is not a Data Lake.", resourceName)
                .getResourceCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        String accountId = restRequestThreadLocalService.getAccountId();
        return stackDtoService.findNotTerminatedByNamesAndAccountId(resourceNames, accountId)
                .stream()
                .filter(stackView -> StackType.DATALAKE.equals(stackView.getType()))
                .map(StackView::getResourceCrn)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        try {
            return Optional.of(getNotTerminatedDatalakeStackViewSafely(() -> stackDtoService.findNotTerminatedByCrn(resourceCrn),
                    "Stack by CRN %s not found", "Stack with CRN %s is not a Data Lake.", resourceCrn)
                    .getEnvironmentCrn());
        } catch (NotFoundException e) {
            LOGGER.error(String.format("Getting environment crn by resource crn %s failed, ", resourceCrn), e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        return stackDtoService.findNotTerminatedByCrns(resourceCrns)
                .stream()
                .filter(stackView -> StackType.DATALAKE.equals(stackView.getType()))
                .collect(Collectors.toMap(StackView::getResourceCrn, stackView -> Optional.ofNullable(stackView.getEnvironmentCrn())));
    }

    private StackView getNotTerminatedDatalakeStackViewSafely(Supplier<Optional<? extends StackView>> optionalStackView, String notFoundMessageTemplate,
            String notDatalakeMessageTemplate, String input) {
        StackView stackView = optionalStackView.get().orElseThrow(() -> new NotFoundException(String.format(notFoundMessageTemplate, input)));
        if (!StackType.DATALAKE.equals(stackView.getType())) {
            throw new BadRequestException(String.format(notDatalakeMessageTemplate, input));
        }
        return stackView;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.DATALAKE);
    }
}
