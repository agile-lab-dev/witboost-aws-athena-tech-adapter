package com.witboost.provisioning.athena.utils.typechecker;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.model.AthenaTypesForIceberg;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcebergTypeChecker implements TypeChecker {

    private final Logger logger = LoggerFactory.getLogger(IcebergTypeChecker.class);

    /**
     * Resolves the column type by handling both primitive types, ARRAY, and MAP.
     * This method checks if the column's type is supported by Iceberg and converts
     * Athena column types into Iceberg-compatible types.
     *
     * @param column the AthenaColumn object that defines the column's name and type
     * @return the resolved Iceberg-compatible type as a String
     * @throws IllegalArgumentException if the column's type is not supported by Iceberg
     *                                  or if required parameters (precision/scale) are missing
     */
    @Override
    public Either<FailedOperation, String> resolveColumnType(AthenaColumn column) {
        try {
            String baseType = column.getDataType().toUpperCase();

            switch (baseType) {
                case "BOOLEAN":
                case "INT":
                case "BIGINT":
                case "DOUBLE":
                case "FLOAT":
                case "STRING":
                case "BINARY":
                case "DATE":
                case "TIMESTAMP":
                case "TIMESTAMPTZ":
                    return Either.right(baseType);
                case "DECIMAL":
                    int precision = Integer.parseInt(column.getPrecision()
                            .map(Object::toString)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Missing precision for DECIMAL column: " + column.getName())));
                    int scale = Integer.parseInt(column.getScale()
                            .map(Object::toString)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Missing scale for DECIMAL column: " + column.getName())));
                    return Either.right("DECIMAL(" + precision + "," + scale + ")");
                case "ARRAY":
                    return Either.right(resolveArrayType(column));
                case "MAP":
                    String keyType = column.getMapKeyType().orElse("UNKNOWN").toUpperCase();
                    String valueType =
                            column.getMapValueType().orElse("UNKNOWN").toUpperCase();
                    return Either.right(resolveMapType(keyType, valueType, column));
                default:
                    throw new IllegalArgumentException(
                            "Unsupported data type for column " + column.getName() + ": " + column.getDataType());
            }
        } catch (Exception e) {
            String error = String.format("Error resolving column type of column %s", column.getName());
            logger.error(error + "Details: " + e.getMessage());
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }

    /**
     * Resolves the type for an ARRAY column, including handling arrays of arrays, arrays of maps,
     * and the application of precision and scale for DECIMAL types.
     *
     * @param column the AthenaColumn object that defines the array column
     * @return the resolved Iceberg-compatible type for the ARRAY column
     * @throws IllegalArgumentException if the array type or nested types are not supported by Iceberg
     *                                  or if required parameters (precision/scale) are missing
     */
    private static String resolveArrayType(AthenaColumn column) {
        // arrayDataType indicates the type of the array elements.
        String innerType = column.getArrayDataType().orElse("UNKNOWN").toUpperCase();
        if (!AthenaTypesForIceberg.isSupported(innerType)) {
            throw new IllegalArgumentException(
                    "Unsupported array element type for column " + column.getName() + ": " + innerType);
        }

        if ("ARRAY".equals(innerType)) {
            // ARRAY of ARRAY: use nestedArrayDataType and its parameters.
            String nestedType =
                    column.getNestedArrayDataType().orElse("UNKNOWN").toUpperCase();
            if (!AthenaTypesForIceberg.isSupported(nestedType)) {
                throw new IllegalArgumentException(
                        "Unsupported nested array element type for column " + column.getName() + ": " + nestedType);
            }
            String resolvedNested =
                    resolveTypeWithParameters(nestedType, column.getNestedPrecision(), column.getNestedScale());
            return "ARRAY<ARRAY<" + resolvedNested + ">>";
        } else if ("MAP".equals(innerType)) {
            // ARRAY of MAP: use the specific arrayMap* parameters.
            String keyType = column.getArrayMapKeyType().orElse("UNKNOWN").toUpperCase();
            String valueType = column.getArrayMapValueType().orElse("UNKNOWN").toUpperCase();
            if (!AthenaTypesForIceberg.isSupported(keyType)) {
                throw new IllegalArgumentException(
                        "Unsupported array map key type for column " + column.getName() + ": " + keyType);
            }
            if (!AthenaTypesForIceberg.isSupported(valueType)) {
                throw new IllegalArgumentException(
                        "Unsupported array map value type for column " + column.getName() + ": " + valueType);
            }
            String resolvedKey =
                    resolveTypeWithParameters(keyType, column.getArrayMapKeyPrecision(), column.getArrayMapKeyScale());
            String resolvedValue = resolveTypeWithParameters(
                    valueType, column.getArrayMapValuePrecision(), column.getArrayMapValueScale());
            return "ARRAY<MAP<" + resolvedKey + "," + resolvedValue + ">>";
        } else {
            // For primitive types (e.g., DECIMAL) in an ARRAY:
            if ("DECIMAL".equals(innerType)) {
                // Prefer the column-level precision/scale over nested ones.
                Optional<String> precisionOpt =
                        column.getPrecision().map(Object::toString).or(() -> column.getNestedPrecision());
                Optional<String> scaleOpt =
                        column.getScale().map(Object::toString).or(() -> column.getNestedScale());
                String finalPrecision = precisionOpt.orElseThrow(() -> new IllegalArgumentException(
                        "Missing precision for DECIMAL array element in column " + column.getName()));
                String finalScale = scaleOpt.orElseThrow(() -> new IllegalArgumentException(
                        "Missing scale for DECIMAL array element in column " + column.getName()));
                String resolvedInner =
                        resolveTypeWithParameters(innerType, Optional.of(finalPrecision), Optional.of(finalScale));
                return "ARRAY<" + resolvedInner + ">";
            } else {
                return "ARRAY<" + innerType + ">";
            }
        }
    }

    /**
     * Resolves the type for a MAP column, applying any parameters for key and value.
     *
     * @param keyType   the type of the map key (e.g., "INT")
     * @param valueType the type of the map value (e.g., "DECIMAL")
     * @param column    the AthenaColumn object that defines the map column
     * @return the resolved Iceberg-compatible type for the MAP column
     * @throws IllegalArgumentException if the map key or value types are not supported by Iceberg
     *                                  or if required parameters (precision/scale) are missing
     */
    private static String resolveMapType(String keyType, String valueType, AthenaColumn column) {
        if (!AthenaTypesForIceberg.isSupported(keyType)) {
            throw new IllegalArgumentException(
                    "Unsupported map key type for column " + column.getName() + ": " + keyType);
        }
        if (!AthenaTypesForIceberg.isSupported(valueType)) {
            throw new IllegalArgumentException(
                    "Unsupported map value type for column " + column.getName() + ": " + valueType);
        }

        String resolvedKey = resolveTypeWithParameters(keyType, column.getMapKeyPrecision(), column.getMapKeyScale());
        String resolvedValue =
                resolveTypeWithParameters(valueType, column.getMapValuePrecision(), column.getMapValueScale());
        return "MAP<" + resolvedKey + "," + resolvedValue + ">";
    }

    /**
     * Adds type parameters for types like DECIMAL (e.g. precision, scale).
     * Throws an exception if the provided type is unknown or if required parameters are missing.
     *
     * @param type          the column type (e.g. "DECIMAL")
     * @param precisionOpt  optional precision for DECIMAL types
     * @param scaleOpt      optional scale for DECIMAL types
     * @return the resolved type string with its parameters
     * @throws IllegalArgumentException if the provided type is unknown or if required parameters are missing
     */
    private static String resolveTypeWithParameters(String type, Optional<?> precisionOpt, Optional<?> scaleOpt) {

        if ("DECIMAL".equals(type)) {
            String precisionStr = precisionOpt
                    .map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing precision for DECIMAL type"));
            String scaleStr = scaleOpt.map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Missing scale for DECIMAL type"));
            int precision = Integer.parseInt(precisionStr);
            int scale = Integer.parseInt(scaleStr);
            return "DECIMAL(" + precision + "," + scale + ")";
        } else {
            return type;
        }
    }
}
