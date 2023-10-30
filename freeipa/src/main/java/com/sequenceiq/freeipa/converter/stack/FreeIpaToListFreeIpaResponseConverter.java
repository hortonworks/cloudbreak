package com.sequenceiq.freeipa.converter.stack;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;

@Component
public class FreeIpaToListFreeIpaResponseConverter {

    @Inject
    private StackToAvailabilityStatusConverter stackToAvailabilityStatusConverter;

    public List<ListFreeIpaResponse> convertList(List<FreeIpaListView> freeIpaList) {
        return freeIpaList.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private ListFreeIpaResponse convert(FreeIpaListView freeIpa) {
        ListFreeIpaResponse response = new ListFreeIpaResponse();
        response.setDomain(freeIpa.domain());
        response.setName(freeIpa.name());
        response.setCrn(freeIpa.resourceCrn());
        response.setEnvironmentCrn(freeIpa.environmentCrn());
        if (freeIpa.stackStatus() != null) {
            response.setStatus(freeIpa.stackStatus().getStatus());
            response.setStatusString(freeIpa.stackStatus().getStatusString());
            response.setAvailabilityStatus(stackToAvailabilityStatusConverter.convert(freeIpa.stackStatus()));
        }
        return response;
    }
}
