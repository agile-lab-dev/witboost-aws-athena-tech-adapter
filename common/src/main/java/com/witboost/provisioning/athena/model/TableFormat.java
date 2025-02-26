package com.witboost.provisioning.athena.model;

public enum TableFormat {
    ICEBERG("iceberg");

    private final String value;

    TableFormat(String value) {
        this.value = value;
    }

    /**
     * Gets the string value associated with the enum.
     *
     * @return the string value of the enum
     */
    public String getValue() {
        return value;
    }
}
