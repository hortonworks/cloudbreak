package com.sequenceiq.mock.clouderamanager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.mock.swagger.model.ApiProductVersion;

@Service
public class ClouderaManagerStoreService {

    private Map<String, ClouderaManagerDto> cmDtos = new HashMap<>();

    public void setClouderaManagerProducts(String mockUuid, List<ApiProductVersion> products) {
        ClouderaManagerDto clouderaManagerDto = new ClouderaManagerDto(mockUuid);
        clouderaManagerDto.setProduct(products);
        cmDtos.putIfAbsent(mockUuid, clouderaManagerDto);
    }

    public List<ApiProductVersion> getClouderaManagerProducts(String mockUuid) {
        return cmDtos.get(mockUuid).getProduct();
    }

    public void terminate(String mockuuid) {
        cmDtos.remove(mockuuid);
    }

    public Collection<ClouderaManagerDto> getAll() {
        return cmDtos.values();
    }
}
