package com.witboost.provisioning.athena.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TableFormatTest {

    @Test
    void testGetValue() {
        assertEquals("iceberg", TableFormat.ICEBERG.getValue());
    }

    @Test
    void testEnumName() {
        assertEquals("ICEBERG", TableFormat.ICEBERG.name());
    }

    @Test
    void testEnumValues() {
        TableFormat[] values = TableFormat.values();
        assertEquals(1, values.length);
        assertEquals(TableFormat.ICEBERG, values[0]);
    }

    @Test
    void testEnumValueOf() {
        TableFormat format = TableFormat.valueOf("ICEBERG");
        assertEquals(TableFormat.ICEBERG, format);
    }
}
