package com.witboost.provisioning.athena.model;

import com.witboost.provisioning.model.Specific;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AthenaSpecific extends Specific {

    @NotBlank
    private String region;

    @Valid
    private TableSpecific source;

    @Valid
    private TableSpecific destination;
}
