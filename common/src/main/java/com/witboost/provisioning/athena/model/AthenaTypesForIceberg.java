package com.witboost.provisioning.athena.model;

public enum AthenaTypesForIceberg {
    BOOLEAN,
    INT,
    BIGINT,
    DOUBLE,
    FLOAT,
    DECIMAL,
    STRING,
    BINARY,
    DATE,
    TIMESTAMP,
    TIMESTAMPTZ,
    ARRAY,
    MAP;

    /**
     * Check if a given Athena type is supported by Iceberg.
     *
     * @param type the Athena type to check
     * @return true if the type is supported by Iceberg, false otherwise
     */
    public static boolean isSupported(String type) {
        try {
            AthenaTypesForIceberg.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
