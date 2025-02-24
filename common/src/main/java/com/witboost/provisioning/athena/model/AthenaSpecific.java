package com.witboost.provisioning.athena.model;

import com.witboost.provisioning.model.Specific;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AthenaSpecific extends Specific {

    @NotBlank
    private String storageAreaId;

    @Valid
    @NotNull
    private AthenaTable sourceTable;

    @Valid
    @NotNull
    private AthenaView view;
}
