package com.witboost.provisioning.athena.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AthenaColumnTest {

    @Test
    void testGetterSetter() {
        AthenaColumn column = new AthenaColumn();

        column.setName("id");
        column.setDescription("The ID column");
        column.setDataType("INT");

        assertEquals("id", column.getName());
        assertEquals("The ID column", column.getDescription());
        assertEquals("INT", column.getDataType());
    }

    @Test
    void testOptionalFields() {
        AthenaColumn column = new AthenaColumn();

        column.setNestedArrayDataType(Optional.of("STRING"));
        column.setNestedScale(Optional.of("10"));
        column.setNestedPrecision(Optional.of("5"));
        column.setNestedDataLength(Optional.of("100"));
        column.setMapKeyDataLength(Optional.of("1"));
        column.setMapValueDataLength(Optional.of("2"));
        column.setArrayMapKeyDataLength(Optional.of("3"));
        column.setArrayMapValueDataLength(Optional.of("4"));

        assertEquals(Optional.of("STRING"), column.getNestedArrayDataType());
        assertEquals(Optional.of("10"), column.getNestedScale());
        assertEquals(Optional.of("5"), column.getNestedPrecision());
        assertEquals(Optional.of("100"), column.getNestedDataLength());
        assertEquals(Optional.of("1"), column.getMapKeyDataLength());
        assertEquals(Optional.of("2"), column.getMapValueDataLength());
        assertEquals(Optional.of("3"), column.getArrayMapKeyDataLength());
        assertEquals(Optional.of("4"), column.getArrayMapValueDataLength());
    }

    @Test
    void testEmptyOptionalFields() {
        AthenaColumn column = new AthenaColumn();

        assertEquals(Optional.empty(), column.getNestedArrayDataType());
        assertEquals(Optional.empty(), column.getNestedScale());
        assertEquals(Optional.empty(), column.getNestedPrecision());
        assertEquals(Optional.empty(), column.getNestedDataLength());
    }

    @Test
    void testArrayMapKeyType() {
        AthenaColumn column = new AthenaColumn();

        column.setArrayMapKeyType(Optional.of("STRING"));

        assertEquals(Optional.of("STRING"), column.getArrayMapKeyType());
    }
}
