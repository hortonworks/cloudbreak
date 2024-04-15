package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistroxEnabledInstanceTypes {

    private static final String ENABLED_TYPES =
        "Standard_D5_v2," +
        "Standard_D8_v3," +
        "Standard_D8_v5," +
        "Standard_D8s_v3," +
        "Standard_D8s_v5," +
        "Standard_D8as_v5," +
        "Standard_D8a_v4," +
        "Standard_D8as_v4," +
        "Standard_D13_v2," +
        "Standard_D14_v2," +
        "Standard_D16_v3," +
        "Standard_D16s_v3," +
        "Standard_D16_v5," +
        "Standard_D16s_v5," +
        "Standard_D16as_v5," +
        "Standard_D16a_v4," +
        "Standard_D16as_v4," +
        "Standard_D32_v3," +
        "Standard_D32s_v3," +
        "Standard_D32_v5," +
        "Standard_D32s_v5," +
        "Standard_D32as_v5," +
        "Standard_D32a_v4," +
        "Standard_D32as_v4," +
        "Standard_D48_v5," +
        "Standard_D48s_v5," +
        "Standard_D48as_v5," +
        "Standard_D64_v3," +
        "Standard_D64s_v3," +
        "Standard_D64a_v4," +
        "Standard_D64as_v4," +
        "Standard_D64s_v5," +
        "Standard_D64_v5," +
        "Standard_D64as_v5," +
        "Standard_E8_v3," +
        "Standard_E16_v3," +
        "Standard_E32_v3," +
        "Standard_F8s_v2," +
        "Standard_F16s_v2," +
        "Standard_F32s_v2," +
        "Standard_L8s_v2," +
        "Standard_L16s_v2," +
        "Standard_L32s_v2," +
        "Standard_L8s," +
        "Standard_L16s," +
        "Standard_L32s," +
        "Standard_L8as_v3," +
        "Standard_NC6," +
        "Standard_NC24r," +
        "Standard_NC6s_v3," +
        "Standard_NC24s_v3," +
        "Standard_L48s_v2," +
        "Standard_E4a_v4," +
        "Standard_E8a_v4," +
        "Standard_E16a_v4," +
        "Standard_E32a_v4," +
        "Standard_E64a_v4," +
        "Standard_E64as_v4," +
        "Standard_E4as_v5," +
        "Standard_E8as_v5," +
        "Standard_E16as_v5," +
        "Standard_E20as_v5," +
        "Standard_E32as_v5," +
        "Standard_E48as_v5," +
        "Standard_E64as_v5," +
        "Standard_E8ds_v4," +
        "Standard_E16ds_v4," +
        "Standard_E32ds_v4," +
        "Standard_E64ds_v4," +
        "Standard_E8s_v3," +
        "Standard_E16s_v3," +
        "Standard_E20s_v3," +
        "Standard_E32s_v3," +
        "Standard_E48s_v3," +
        "Standard_E64s_v3," +
        "Standard_E8s_v4," +
        "Standard_E16s_v4," +
        "Standard_E32s_v4," +
        "Standard_D4s_v3," +
        "Standard_D48s_v3," +
        "Standard_E4_v3," +
        "Standard_E4s_v3," +
        "Standard_E4s_v4," +
        "Standard_E4ds_v4," +
        "Standard_E16-8s_v3," +
        "Standard_E20s_v4," +
        "Standard_E20ds_v4," +
        "Standard_E48ds_v4," +
        "Standard_E48s_v4";

    public static final List<String> AZURE_ENABLED_TYPES_LIST = new ArrayList<String>(Arrays.asList(ENABLED_TYPES.trim().split(",")));

    private DistroxEnabledInstanceTypes() {
    }

}
