package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Service;

@Component
public class ServiceFindResponse extends AbstractFreeIpaResponse<Set<Service>> {
    @Override
    public String method() {
        return "service_find";
    }

    @Override
    protected Set<Service> handleInternal(String body) {
        Service service = new Service();
        service.setDn("admin");
        service.setKrbprincipalname(List.of("dummy"));
        service.setKrbcanonicalname("dummy");
        return Set.of(service);
    }
}
