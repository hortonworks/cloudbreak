package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class FreeIpaDescribeService {

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    public DescribeFreeIpaResponse describe(String environmentCrn) {
        Stack stack = stackService.getByEnvironmentCrnWithLists(environmentCrn);
        Image image = imageService.getByStack(stack);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        return stackToDescribeFreeIpaResponseConverter.convert(stack, image, freeIpa);
    }
}
