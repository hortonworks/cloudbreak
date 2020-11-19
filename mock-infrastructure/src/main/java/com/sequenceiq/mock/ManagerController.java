package com.sequenceiq.mock;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.freeipa.FreeIpaDto;
import com.sequenceiq.mock.freeipa.FreeipaStoreService;
import com.sequenceiq.mock.salt.SaltDto;
import com.sequenceiq.mock.salt.SaltStoreService;
import com.sequenceiq.mock.spi.SpiDto;
import com.sequenceiq.mock.spi.SpiStoreService;

@RestController
@RequestMapping("/mocks")
public class ManagerController {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private FreeipaStoreService freeipaStoreService;

    @Inject
    private SaltStoreService saltStoreService;

    @GetMapping("/spi")
    public Collection<SpiDto> spiDtos() {
        return spiStoreService.getAll();
    }

    @GetMapping("/spi/{mockUuid}")
    public SpiDto spiDto(@PathVariable String mockUuid) {
        return spiStoreService.read(mockUuid);
    }

    @GetMapping("/salt")
    public Collection<SaltDto> saltDtos() {
        return saltStoreService.getAll();
    }

    @GetMapping("/cm")
    public Collection<ClouderaManagerDto> cmDtos() {
        return clouderaManagerStoreService.getAll();
    }

    @GetMapping("/freeipa")
    public Collection<FreeIpaDto> freeipaDtos() {
        return freeipaStoreService.getAll();
    }

}
