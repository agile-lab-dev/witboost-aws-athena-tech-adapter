package com.witboost.provisioning.athena.utils.typechecker;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IcebergTypeCheckerTest {

    private IcebergTypeChecker typeChecker;

    @BeforeEach
    public void setUp() {
        typeChecker = new IcebergTypeChecker();
    }

    @Test
    public void testPrimitiveType_INT() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_int");
        column.setDataType("INT");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("INT", result.get());
    }

    @Test
    public void testPrimitiveType_BOOLEAN() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_boolean");
        column.setDataType("BOOLEAN");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("BOOLEAN", result.get());
    }

    @Test
    public void testPrimitiveType_BIGINT() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_bigint");
        column.setDataType("BIGINT");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("BIGINT", result.get());
    }

    @Test
    public void testPrimitiveType_DOUBLE() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_double");
        column.setDataType("DOUBLE");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("DOUBLE", result.get());
    }

    @Test
    public void testPrimitiveType_FLOAT() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_float");
        column.setDataType("FLOAT");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("FLOAT", result.get());
    }

    @Test
    public void testPrimitiveType_STRING() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_string");
        column.setDataType("STRING");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("STRING", result.get());
    }

    @Test
    public void testPrimitiveType_BINARY() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_binary");
        column.setDataType("BINARY");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("BINARY", result.get());
    }

    @Test
    public void testPrimitiveType_DATE() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_date");
        column.setDataType("DATE");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("DATE", result.get());
    }

    @Test
    public void testPrimitiveType_TIMESTAMP() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_timestamp");
        column.setDataType("TIMESTAMP");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("TIMESTAMP", result.get());
    }

    @Test
    public void testPrimitiveType_TIMESTAMPTZ() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_timestamptz");
        column.setDataType("TIMESTAMPTZ");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("TIMESTAMPTZ", result.get());
    }

    @Test
    public void testDecimalType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_decimal");
        column.setDataType("DECIMAL");
        column.setPrecision(Optional.of(10));
        column.setScale(Optional.of(2));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("DECIMAL(10,2)", result.get());
    }

    @Test
    public void testArrayOfPrimitive() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_int");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("INT"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("ARRAY<INT>", result.get());
    }

    @Test
    public void testArrayOfArray() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_of_array");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("ARRAY"));
        column.setNestedArrayDataType(Optional.of("DECIMAL"));
        column.setNestedPrecision(Optional.of("5"));
        column.setNestedScale(Optional.of("15"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("ARRAY<ARRAY<DECIMAL(5,15)>>", result.get());
    }

    @Test
    public void testArrayOfMap() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_of_map");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("MAP"));
        column.setArrayMapKeyType(Optional.of("INT"));
        column.setArrayMapValueType(Optional.of("DECIMAL"));
        column.setArrayMapKeyPrecision(Optional.empty());
        column.setArrayMapKeyScale(Optional.empty());
        column.setArrayMapValuePrecision(Optional.of("8"));
        column.setArrayMapValueScale(Optional.of("3"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("ARRAY<MAP<INT,DECIMAL(8,3)>>", result.get());
    }

    @Test
    public void testMapType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("test_map");
        column.setDataType("MAP");
        column.setMapKeyType(Optional.of("DECIMAL"));
        column.setMapValueType(Optional.of("DECIMAL"));
        column.setMapValuePrecision(Optional.of("12"));
        column.setMapValueScale(Optional.of("4"));
        column.setMapKeyScale(Optional.of("12"));
        column.setMapKeyPrecision(Optional.of("15"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isRight());
        assertEquals("MAP<DECIMAL(15,12),DECIMAL(12,4)>", result.get());
    }

    @Test
    public void testUnsupportedType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("unsupported");
        column.setDataType("MY_TYPE");
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assert result.getLeft().problems().get(0).getMessage().contains("unsupported: MY_TYPE");
    }

    @Test
    public void testDecimalMissingScale() {
        AthenaColumn column = new AthenaColumn();
        column.setName("decimal_missing");
        column.setDataType("DECIMAL");
        column.setPrecision(Optional.of(12));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assert result.getLeft().problems().get(0).getMessage().contains("Missing scale");
    }

    @Test
    public void testDecimalMissingPrecision() {
        AthenaColumn column = new AthenaColumn();
        column.setName("decimal_missing");
        column.setDataType("DECIMAL");
        column.setScale(Optional.of(4));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assert result.getLeft().problems().get(0).getMessage().contains("Missing precision");
    }

    @Test
    public void testUnsupportedArrayElementType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_unsupported");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("MY_TYPE"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported array element type"));
    }

    @Test
    public void testUnsupportedNestedArrayElementType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_nested_unsupported");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("ARRAY"));
        column.setNestedArrayDataType(Optional.of("MY_TYPE"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported nested array element type"));
    }

    @Test
    public void testUnsupportedArrayMapKeyType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_map_key_unsupported");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("MAP"));
        column.setArrayMapKeyType(Optional.of("MY_TYPE"));
        column.setArrayMapValueType(Optional.of("DECIMAL"));
        column.setArrayMapValuePrecision(Optional.of("8"));
        column.setArrayMapValueScale(Optional.of("3"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported array map key type"));
    }

    @Test
    public void testUnsupportedArrayMapValueType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_map_value_unsupported");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("MAP"));
        column.setArrayMapKeyType(Optional.of("INT"));
        column.setArrayMapValueType(Optional.of("MY_TYPE"));
        column.setArrayMapValuePrecision(Optional.of("8"));
        column.setArrayMapValueScale(Optional.of("3"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported array map value type"));
    }

    @Test
    public void testUnsupportedMapKeyType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("map_key_unsupported");
        column.setDataType("MAP");
        column.setMapKeyType(Optional.of("MY_TYPE"));
        column.setMapValueType(Optional.of("DECIMAL"));
        column.setMapValuePrecision(Optional.of("12"));
        column.setMapValueScale(Optional.of("4"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported map key type"));
    }

    @Test
    public void testUnsupportedMapValueType() {
        AthenaColumn column = new AthenaColumn();
        column.setName("map_value_unsupported");
        column.setDataType("MAP");
        column.setMapKeyType(Optional.of("INT"));
        column.setMapValueType(Optional.of("MY_TYPE"));
        column.setMapValuePrecision(Optional.of("12"));
        column.setMapValueScale(Optional.of("4"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Unsupported map value type"));
    }

    @Test
    public void testArrayDecimalMissingParameters() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_decimal_missing");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("DECIMAL"));
        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Missing precision"));
    }

    @Test
    public void testMapTypeDecimalMissingParameters() {
        AthenaColumn column = new AthenaColumn();
        column.setName("map_decimal_missing");
        column.setDataType("MAP");
        column.setMapKeyType(Optional.of("DECIMAL"));
        column.setMapValueType(Optional.of("DECIMAL"));
        column.setMapKeyPrecision(Optional.of("10"));
        column.setMapValuePrecision(Optional.of("12"));
        column.setMapValueScale(Optional.of("4"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().problems().get(0).getMessage().contains("Missing scale"));
    }

    @Test
    public void testDecimalArrayMissingScale() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_decimal_missing_scale");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("DECIMAL"));
        column.setNestedPrecision(Optional.of("10"));
        column.setNestedScale(Optional.empty());

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);

        assertTrue(result.isLeft());
        assertTrue(result.getLeft()
                .problems()
                .get(0)
                .getMessage()
                .contains("Missing scale for DECIMAL array element in column array_decimal_missing_scale"));
    }

    @Test
    public void testDecimalArrayMissingPrecision() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_decimal_missing_scale");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("DECIMAL"));
        column.setNestedScale(Optional.of("10"));
        column.setNestedPrecision(Optional.empty());

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);

        assertTrue(result.isLeft());
        assertTrue(result.getLeft()
                .problems()
                .get(0)
                .getMessage()
                .contains("Missing precision for DECIMAL array element in column array_decimal_missing_scale"));
    }

    @Test
    public void testArrayDecimalWithPrecisionAndScale() {
        AthenaColumn column = new AthenaColumn();
        column.setName("array_decimal_with_precision_scale");
        column.setDataType("ARRAY");
        column.setArrayDataType(Optional.of("DECIMAL"));
        column.setNestedPrecision(Optional.of("10"));
        column.setNestedScale(Optional.of("3"));

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);

        assertTrue(result.isRight());
        assertEquals("ARRAY<DECIMAL(10,3)>", result.get());
    }

    @Test
    public void testUnsupportedDataTypeDefaultBranch() {
        AthenaColumn column = new AthenaColumn();
        column.setName("unsupported_column");
        column.setDataType("UNSUPPORTED_TYPE");

        Either<FailedOperation, String> result = typeChecker.resolveColumnType(column);

        assertTrue(result.isLeft());
        assertTrue(result.getLeft()
                .problems()
                .get(0)
                .getMessage()
                .contains("Unsupported data type for column unsupported_column: UNSUPPORTED_TYPE"));
    }
}
