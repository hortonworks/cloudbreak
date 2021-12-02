package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.CsdSegments.segments;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class CsdParcelNameMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsdParcelNameMatcher.class);

    private Map<String, CsdSegments> parcelCsdSegmentsMapping = Map.of(
            "CFM", segments("NIFI", "NIFIREGISTRY"),
            "FLINK", segments("FLINK", "SQL_STREAM_BUILDER")
    );

    public boolean matching(String csdUrl, String parcelName) {
        boolean nameMatchingWithParcel;
        Optional<CsdSegments> matchingSegments = getMatchingSegments(parcelName);
        if (matchingSegments.isPresent()) {
            LOGGER.trace("{} parcel name was found in the mapping and now searching the component name in the csd url '{}'",
                    parcelName, csdUrl);
            Optional<String> firstComponentWhichIsMatching = matchingSegments.get().getComponentList()
                    .stream()
                    .filter(segment -> matchingString(csdUrl, segment))
                    .findFirst();
            nameMatchingWithParcel = firstComponentWhichIsMatching.isPresent();
        } else {
            LOGGER.trace("{} parcel name was NOT found in the mapping and now searching the parcel name in the csd url '{}'",
                    parcelName, csdUrl);
            nameMatchingWithParcel = matchingString(csdUrl, parcelName);
        }
        return nameMatchingWithParcel;
    }

    private Optional<CsdSegments> getMatchingSegments(String parcelName) {
        if (!Strings.isNullOrEmpty(parcelName)) {
            return Optional.ofNullable(parcelCsdSegmentsMapping.get(parcelName.toUpperCase()));
        }
        return Optional.empty();
    }

    private boolean matchingString(String csdUrl, String component) {
        return !Strings.isNullOrEmpty(csdUrl)
                && !Strings.isNullOrEmpty(component)
                && csdUrl.toLowerCase().contains(component.toLowerCase());
    }
}
