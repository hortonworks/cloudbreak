package com.sequenceiq.cloudbreak.blueprint.template;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.sequenceiq.cloudbreak.blueprint.template.views.GatewayView;

public class GatewayDisabledHelper implements Helper<GatewayView> {

    /**
     * A singleton instance of this helper.
     */
    public static final Helper<GatewayView> INSTANCE = new GatewayDisabledHelper();

    /**
     * The helper's name.
     */
    public static final String NAME = "gateway-disabled";

    @Override
    public Object apply(GatewayView context, Options options) throws IOException {
        Options.Buffer buffer = options.buffer();
        if (context == null || !context.getEnableGateway()) {
            buffer.append(options.fn());
        } else {
            buffer.append(options.inverse());
        }
        return buffer;
    }
}
