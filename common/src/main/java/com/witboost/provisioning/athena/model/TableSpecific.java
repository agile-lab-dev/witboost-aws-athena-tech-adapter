package com.witboost.provisioning.athena.model;

import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TableSpecific {

    @NotBlank
    String catalog;

    @NotBlank
    String database;

    @NotBlank
    String name;
}
