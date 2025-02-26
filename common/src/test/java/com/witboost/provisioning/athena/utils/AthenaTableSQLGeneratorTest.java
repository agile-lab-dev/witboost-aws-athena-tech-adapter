package com.witboost.provisioning.athena.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.athena.utils.typechecker.TypeChecker;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AthenaTableSQLGeneratorTest {

    private TypeChecker typeChecker;
    private AthenaTableSQLGenerator sqlGenerator;

    @BeforeEach
    void setUp() {
        // Mock the TypeChecker
        typeChecker = Mockito.mock(TypeChecker.class);
        sqlGenerator = new AthenaTableSQLGenerator(typeChecker);
    }

    @Test
    void testGenerateCreateTableSQL_Success() {
        AthenaColumn column = new AthenaColumn();
        column.setName("id");
        column.setDescription("The ID column");
        column.setDataType("INT");

        Mockito.when(typeChecker.resolveColumnType(column)).thenReturn(Either.right("INT"));

        Either<FailedOperation, String> result = sqlGenerator.generateCreateTableSQL(
                "my_database", "my_table", "ICEBERG", "s3://bucket/my_table", List.of(column));

        assertTrue(result.isRight());
        String expectedSQL = "CREATE TABLE my_database.my_table (\n  id INT\n)\n"
                + "LOCATION 's3://bucket/my_table'\n"
                + "TBLPROPERTIES ( 'table_type' = 'ICEBERG' )\n";
        assertEquals(expectedSQL, result.get());
    }

    @Test
    void testGenerateCreateTableSQL_TypeResolutionFailure() {
        AthenaColumn column = new AthenaColumn();
        column.setName("id");
        column.setDescription("The ID column");
        column.setDataType("UNKNOWN_TYPE");

        Mockito.when(typeChecker.resolveColumnType(column))
                .thenReturn(Either.left(
                        new FailedOperation("Unknown type", List.of(new Problem("Type resolution failed")))));

        Either<FailedOperation, String> result = sqlGenerator.generateCreateTableSQL(
                "my_database", "my_table", "ICEBERG", "s3://bucket/my_table", List.of(column));

        assertTrue(result.isLeft());
        FailedOperation failedOperation = result.getLeft();
        assertEquals("Unknown type", failedOperation.message());
    }

    @Test
    void testGenerateCreateTableSQL_ExceptionHandling() {
        Mockito.when(typeChecker.resolveColumnType(Mockito.any())).thenThrow(new RuntimeException("Unexpected error"));

        Either<FailedOperation, String> result = sqlGenerator.generateCreateTableSQL(
                "my_database", "my_table", "ICEBERG", "s3://bucket/my_table", List.of(new AthenaColumn()));

        assertTrue(result.isLeft());
        FailedOperation failedOperation = result.getLeft();
        assertTrue(failedOperation.message().contains("An unexpected error occurred"));
    }
}
