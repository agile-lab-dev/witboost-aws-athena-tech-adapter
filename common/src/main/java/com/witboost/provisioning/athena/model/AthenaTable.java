package com.witboost.provisioning.athena.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AthenaTable extends AbstractAthenaEntity {

    @NotNull
    private TableFormat tableFormat;
}
