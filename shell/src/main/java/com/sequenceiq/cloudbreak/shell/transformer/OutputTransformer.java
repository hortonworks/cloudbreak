package com.sequenceiq.cloudbreak.shell.transformer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.NotSupportedException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.support.JsonRenderer;
import com.sequenceiq.cloudbreak.shell.support.TableRenderer;

@Component
public class OutputTransformer {

    @Inject
    private TableRenderer tableRenderer;

    @Inject
    private JsonRenderer jsonRenderer;

    public <O> String render(OutPutType outPutType, O object, String... headers) throws JsonProcessingException {
        if (OutPutType.JSON.equals(outPutType)) {
            return jsonRenderer.render(object);
        } else if (OutPutType.RAW.equals(outPutType)) {
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                if (!map.values().isEmpty()) {
                    Object value = map.values().stream().filter(e -> e != null).findAny().orElse(null);
                    if (value instanceof Collection) {
                        return tableRenderer.renderMultiValueMap((Map<String, List<String>>) object, true, headers);
                    } else if (value instanceof String) {
                        return tableRenderer.renderSingleMapWithSortedColumn((Map<Object, String>) object, headers);
                    } else {
                        return tableRenderer.renderObjectValueMap((Map<String, Object>) object, headers[0]);
                    }
                } else {
                    return "No available entity";
                }
            } else {
                return "No available entity";
            }
        } else {
            throw new NotSupportedException("Output type not supported.");
        }
    }

    public <O> String render(O object, String... headers) throws JsonProcessingException {
        return render(OutPutType.RAW, object, headers);
    }

}
