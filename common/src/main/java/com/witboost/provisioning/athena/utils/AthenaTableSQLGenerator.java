package com.witboost.provisioning.athena.utils;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.utils.typechecker.TypeChecker;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AthenaTableSQLGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AthenaTableSQLGenerator.class);

    private final TypeChecker typeChecker;

    public AthenaTableSQLGenerator(TypeChecker typeChecker) {
        this.typeChecker = typeChecker;
    }

    /**
     * Generates a CREATE TABLE SQL statement for an Athena table, including the table schema,
     * table format, and location.
     * <p>
     * This method creates the SQL required to create a table in Amazon Athena with the specified
     * database, table name, format, and S3 location. It processes the columns and their types
     * according to the supported Athena types and formats.
     * </p>
     *
     * @param databaseName name of the database where the table will be created
     * @param tableName    name of the table to create
     * @param tableFormat  format of the table (e.g. "PARQUET", "ORC", "TEXTFILE", etc.)
     * @param location     S3 path where the table data is stored
     * @param columns      list of AthenaColumn objects that define the table columns, including their types and properties
     * @return an Either containing either a FailedOperation with an error message or the SQL string for the CREATE TABLE statement
     */
    public Either<FailedOperation, String> generateCreateTableSQL(
            String databaseName, String tableName, String tableFormat, String location, List<AthenaColumn> columns) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ")
                    .append(databaseName)
                    .append(".")
                    .append(tableName)
                    .append(" (\n");

            for (int i = 0; i < columns.size(); i++) {
                AthenaColumn col = columns.get(i);

                Either<FailedOperation, String> typeResult = typeChecker.resolveColumnType(col);
                if (typeResult.isLeft()) {
                    return typeResult;
                }
                String resolvedType = typeResult.get();
                sb.append("  ").append(col.getName()).append(" ").append(resolvedType);
                if (i < columns.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(")\n");
            sb.append("LOCATION '").append(location).append("'\n");
            sb.append("TBLPROPERTIES ( 'table_type' = '")
                    .append(tableFormat.toUpperCase())
                    .append("' )\n");

            return Either.right(sb.toString());
        } catch (Exception e) {
            String error = String.format("An unexpected error occurred during query generation: %s", e.getMessage());
            logger.error(error, e);
            return Either.left(new FailedOperation(error, List.of(new Problem(error, e))));
        }
    }
}
