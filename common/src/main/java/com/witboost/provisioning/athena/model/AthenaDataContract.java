package com.witboost.provisioning.athena.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.witboost.provisioning.model.ServiceLevelAgreements;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/**
 * Representing an Athena Output Port Data Contract, used to parse the {@code dataContract} field on Athena Output Ports.
 * It provides the base fields of the specification, and an {@code additionalProperties} map for additional non-mapped fields
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AthenaDataContract {
    private List<AthenaColumn> schema;
    private Optional<String> termsAndConditions = Optional.empty();
    private Optional<ServiceLevelAgreements> SLA = Optional.empty();
    private Optional<String> endpoint = Optional.empty();
    private Optional<String> biTempBusinessTs = Optional.empty();
    private Optional<String> biTempWriteTs = Optional.empty();

    @JsonAnySetter
    @JsonAnyGetter
    private Map<String, JsonNode> additionalProperties;
}
