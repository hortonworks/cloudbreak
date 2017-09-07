package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.PlatformImagesJson;
import com.sequenceiq.cloudbreak.cloud.model.CustomImage;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;

@Component
public class PlatformImagesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformImages, PlatformImagesJson> {

    @Override
    public PlatformImagesJson convert(PlatformImages source) {
        PlatformImagesJson json = new PlatformImagesJson();
        Map<String, Map<String, String>> images = new HashMap<>();
        for (Entry<Platform, Collection<CustomImage>> platformCollectionEntry : source.getImages().entrySet()) {
            Map<String, String> tmp = new HashMap<>();
            for (CustomImage customImage : platformCollectionEntry.getValue()) {
                tmp.put(customImage.value(), customImage.getImage());
            }
            images.put(platformCollectionEntry.getKey().value(), tmp);
        }
        Map<String, String> regex = new HashMap<>();
        for (Entry<Platform, String> platformStringEntry : source.getRegex().entrySet()) {
            regex.put(platformStringEntry.getKey().value(), platformStringEntry.getValue());
        }
        json.setImages(images);
        json.setImagesRegex(regex);
        return json;
    }
}
