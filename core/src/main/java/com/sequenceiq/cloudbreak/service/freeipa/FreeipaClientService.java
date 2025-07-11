package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;

@Service
public class FreeipaClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaClientService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private FreeIpaV1FlowEndpoint freeIpaV1FlowEndpoint;

    @Inject
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Inject
    private UtilV1Endpoint utilV1Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DescribeFreeIpaResponse getByEnvironmentCrn(String environmentCrn) {
        try {
            if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
                LOGGER.info("The user CRN is internal CRN, so we call freeipa on internal endpoint");
                String accountId = Crn.fromString(environmentCrn).getAccountId();
                return freeIpaV1Endpoint.describeInternal(environmentCrn, accountId);
            } else {
                return freeIpaV1Endpoint.describe(environmentCrn);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET FreeIPA by environment CRN: %s, due to: %s. %s.", environmentCrn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET FreeIPA by environment CRN: %s, due to: %s.", environmentCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public Optional<DescribeFreeIpaResponse> findByEnvironmentCrn(String environmentCrn) {
        try {
            if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
                LOGGER.info("The user CRN is internal CRN, so we call freeipa on internal endpoint");
                String accountId = Crn.fromString(environmentCrn).getAccountId();
                return Optional.ofNullable(freeIpaV1Endpoint.describeInternal(environmentCrn, accountId));
            }
            return Optional.ofNullable(freeIpaV1Endpoint.describe(environmentCrn));
        } catch (NotFoundException e) {
            LOGGER.info("FreeIPA is not found for env: {}", environmentCrn, e);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET FreeIPA by environment CRN: %s, due to: %s. %s.", environmentCrn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET FreeIPA by environment CRN: %s, due to: %s.", environmentCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public List<ListFreeIpaResponse> list() {
        try {
            return freeIpaV1Endpoint.list();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to LIST FreeIPA due to: %s. %s.", e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to LIST FreeIPA due to: %s. ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public List<String> recipes(String accountId) {
        return utilV1Endpoint.usedRecipes(accountId);
    }

    public String getRootCertificateByEnvironmentCrn(String environmentCrn) {
        try {
            if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
                LOGGER.info("The user CRN is internal CRN, so we call freeipa on internal endpoint");
                String accountId = Crn.fromString(environmentCrn).getAccountId();
                return freeIpaV1Endpoint.getRootCertificateInternal(environmentCrn, accountId);
            } else {
                return freeIpaV1Endpoint.getRootCertificate(environmentCrn);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET FreeIPA root certificate by environment CRN: %s, due to: %s. %s.", environmentCrn, e.getMessage(),
                    errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET FreeIPA root certificate by environment CRN: %s, due to: %s.", environmentCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public FlowIdentifier rotateSecret(String envirionmentCrn, FreeIpaSecretRotationRequest request) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> freeIpaRotationV1Endpoint.rotateSecretsByCrn(envirionmentCrn, request));
    }

    public FlowCheckResponse hasFlowRunningByFlowId(String flowId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> freeIpaV1FlowEndpoint.hasFlowRunningByFlowId(flowId));
    }

    public FlowCheckResponse hasFlowChainRunningByFlowChainId(String flowChainId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> freeIpaV1FlowEndpoint.hasFlowRunningByChainId(flowChainId));
    }

    public FlowLogResponse getLastFlowId(String resourceCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> freeIpaV1FlowEndpoint.getLastFlowByResourceCrn(resourceCrn));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw e;
        }
    }

}
