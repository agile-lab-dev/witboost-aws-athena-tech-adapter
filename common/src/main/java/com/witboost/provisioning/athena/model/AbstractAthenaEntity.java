package com.witboost.provisioning.athena.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractAthenaEntity {
    @NotBlank
    private String catalog;

    @NotBlank
    private String database;

    @NotBlank
    private String name;
}
