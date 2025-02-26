package com.witboost.provisioning.athena.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.witboost.provisioning.model.Column;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AthenaColumn extends Column {

    private Optional<String> nestedArrayDataType = Optional.empty();
    private Optional<String> nestedScale = Optional.empty();
    private Optional<String> nestedPrecision = Optional.empty();
    private Optional<String> nestedDataLength = Optional.empty();

    private Optional<String> arrayMapKeyType = Optional.empty();
    private Optional<String> arrayMapValueType = Optional.empty();
    private Optional<String> arrayMapKeyScale = Optional.empty();
    private Optional<String> arrayMapKeyPrecision = Optional.empty();
    private Optional<String> arrayMapKeyDataLength = Optional.empty();
    private Optional<String> arrayMapValueScale = Optional.empty();
    private Optional<String> arrayMapValuePrecision = Optional.empty();
    private Optional<String> arrayMapValueDataLength = Optional.empty();
    private Optional<String> mapKeyType = Optional.empty();
    private Optional<String> mapValueType = Optional.empty();
    private Optional<String> mapKeyDataLength = Optional.empty();
    private Optional<String> mapValueDataLength = Optional.empty();
    private Optional<String> mapKeyPrecision = Optional.empty();
    private Optional<String> mapValuePrecision = Optional.empty();
    private Optional<String> mapKeyScale = Optional.empty();
    private Optional<String> mapValueScale = Optional.empty();
}
