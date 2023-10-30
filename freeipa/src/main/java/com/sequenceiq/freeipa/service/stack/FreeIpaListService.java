package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.common.model.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.FreeIpaToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class FreeIpaListService {

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaToListFreeIpaResponseConverter freeIpaToListFreeIpaResponseConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public List<ListFreeIpaResponse> list(String accountId) {
        List<FreeIpaListView> stackList = freeIpaService.getAllViewByAccountId(accountId);
        return freeIpaToListFreeIpaResponseConverter.convertList(stackList);
    }

    public List<ResourceWithId> listAsAuthorizationResources(String accountId) {
        return freeIpaService.getAllAsAuthorizationResources(accountId);
    }

    public List<ListFreeIpaResponse> listAllByIds(List<Long> ids) {
        List<FreeIpaListView> stackList = freeIpaService.getAllViewByIds(ids);
        return freeIpaToListFreeIpaResponseConverter.convertList(stackList);
    }

    public List<SubnetIdWithResourceNameAndCrn> getAllUsedSubnetsByEnvironmentCrn(String environmentCrn) {
        return instanceMetaDataService.findAllUsedSubnetsByEnvironmentCrn(environmentCrn);
    }
}
