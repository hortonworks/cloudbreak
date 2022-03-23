package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class FreeipaClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaClientService.class);

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public DescribeFreeIpaResponse getByEnvironmentCrn(String environmentCrn) {
        try {
            return freeIpaV1Endpoint.describe(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET FreeIPA by environment crn: %s, due to: %s. %s.", environmentCrn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET FreeIPA by environment crn: %s, due to: %s.", environmentCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public Optional<DescribeFreeIpaResponse> findByEnvironmentCrn(String environmentCrn) {
        try {
            if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
                String accountId = Crn.fromString(environmentCrn).getAccountId();
                return Optional.ofNullable(freeIpaV1Endpoint.describeInternal(environmentCrn, accountId));
            }
            return Optional.ofNullable(freeIpaV1Endpoint.describe(environmentCrn));
        } catch (NotFoundException e) {
            LOGGER.info("FreeIPA is not found for env: {}", environmentCrn, e);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET FreeIPA by environment crn: %s, due to: %s. %s.", environmentCrn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET FreeIPA by environment crn: %s, due to: %s.", environmentCrn, e.getMessage());
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

    public OperationStatus createBindUsers(BindUserCreateRequest request, String initiatorUserCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> freeIpaV1Endpoint.createBindUser(request, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to invoke bind user creation due to: %s. %s.", e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to invoke bind user creation due to: %s. ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
