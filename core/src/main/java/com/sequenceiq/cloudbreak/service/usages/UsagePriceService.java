package com.sequenceiq.cloudbreak.service.usages;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.price.PriceGenerator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

@Service
public class UsagePriceService {

    @Inject
    private List<PriceGenerator> priceGenerators;

    public Double calculateCostOfUsage(CloudbreakUsage usage) {
        PriceGenerator pg = selectPriceGeneratorByPlatform(platform(usage.getProvider()));
        Double result = 0.0;
        if (pg != null) {
            result = pg.calculate(usage.getInstanceType(), usage.getInstanceHours());
        }
        return result;
    }

    private PriceGenerator selectPriceGeneratorByPlatform(Platform cloudPlatform) {
        PriceGenerator result = null;
        for (PriceGenerator generator : priceGenerators) {
            Platform generatorCloudPlatform = generator.getCloudPlatform();
            if (cloudPlatform.equals(generatorCloudPlatform)) {
                result = generator;
                break;
            }
        }
        return result;
    }
}
