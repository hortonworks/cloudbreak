package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ManagementPackEndpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;

@Component
@Transactional(TxType.NEVER)
public class ManagementPackController extends NotificationController implements ManagementPackEndpoint {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private ManagementPackService mpackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public ManagementPackResponse get(Long id) {
        ManagementPack mpack = mpackService.get(id);
        return conversionService.convert(mpack, ManagementPackResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> mpackService.delete(id, user), ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    @Override
    public ManagementPackResponse postPrivate(ManagementPackRequest mpackRequest) {
        return post(mpackRequest);
    }

    @Override
    public Set<ManagementPackResponse> getPrivates() {
        return getAll();
    }

    @Override
    public ManagementPackResponse getPrivate(String name) {
        return getByName(name);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> mpackService.delete(name, user), ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    @Override
    public ManagementPackResponse postPublic(ManagementPackRequest mpackRequest) {
        return post(mpackRequest);
    }

    @Override
    public Set<ManagementPackResponse> getPublics() {
        return getAll();
    }

    @Override
    public ManagementPackResponse getPublic(String name) {
        return getByName(name);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> mpackService.delete(name, user), ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    private ManagementPackResponse createMpack(IdentityUser user, ManagementPackRequest mpackRequest) {
        ManagementPack mpack = conversionService.convert(mpackRequest, ManagementPack.class);
        try {
            mpack = mpackService.create(user, mpack);
            notify(user, ResourceEvent.MANAGEMENT_PACK_CREATED);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.MANAGEMENT_PACK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return conversionService.convert(mpack, ManagementPackResponse.class);
    }

    private Set<ManagementPackResponse> toJsonList(Set<ManagementPack> mpacks) {
        return (Set<ManagementPackResponse>) conversionService.convert(mpacks,
                TypeDescriptor.forObject(mpacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(ManagementPackResponse.class)));
    }

    private ManagementPackResponse post(ManagementPackRequest mpackRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createMpack(user, mpackRequest);
    }

    private Set<ManagementPackResponse> getAll() {
        return toJsonList(mpackService.listForUsersDefaultOrganization());
    }

    private ManagementPackResponse getByName(String name) {
        return conversionService.convert(mpackService.getByNameFromUsersDefaultOrganization(name), ManagementPackResponse.class);
    }
}
