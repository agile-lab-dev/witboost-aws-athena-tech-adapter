package com.witboost.provisioning.athena.utils.typechecker;

import com.witboost.provisioning.athena.model.TableFormat;
import java.util.HashMap;
import java.util.Map;

public class TypeCheckerFactory {

    private static final Map<TableFormat, TypeChecker> typeCheckerMap = new HashMap<>();

    static {
        // Registration of supported TypeCheckers
        typeCheckerMap.put(TableFormat.ICEBERG, new IcebergTypeChecker());
    }

    /**
     * Returns the correct TypeChecker based on the TableFormat.
     *
     * @param tableFormat the table format
     * @return the corresponding TypeChecker instance
     * @throws IllegalArgumentException if the format is not supported
     */
    public static TypeChecker getTypeChecker(TableFormat tableFormat) {
        if (tableFormat == null) {
            throw new IllegalArgumentException("TableFormat cannot be null");
        }

        TypeChecker typeChecker = typeCheckerMap.get(tableFormat);

        if (typeChecker == null) {
            throw new IllegalArgumentException("No TypeChecker available for TableFormat: " + tableFormat);
        }

        return typeChecker;
    }
}
