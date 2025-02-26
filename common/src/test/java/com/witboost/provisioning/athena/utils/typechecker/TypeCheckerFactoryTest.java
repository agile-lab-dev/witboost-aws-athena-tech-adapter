package com.witboost.provisioning.athena.utils.typechecker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.witboost.provisioning.athena.model.TableFormat;
import org.junit.jupiter.api.Test;

class TypeCheckerFactoryTest {

    @Test
    void testGetTypeCheckerForValidFormat() {
        TypeChecker typeChecker = TypeCheckerFactory.getTypeChecker(TableFormat.ICEBERG);

        assertNotNull(typeChecker, "TypeChecker should not be null");
        assertTrue(
                typeChecker instanceof IcebergTypeChecker, "TypeChecker should be an instance of IcebergTypeChecker");
    }

    @Test
    void testGetTypeCheckerForNullFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TypeCheckerFactory.getTypeChecker(null),
                "Expected IllegalArgumentException for null TableFormat");

        assertEquals("TableFormat cannot be null", exception.getMessage());
    }

    @Test
    void testGetTypeCheckerForUnsupportedFormat() {
        // Create a mock of TableFormat
        TableFormat unsupportedFormat = mock(TableFormat.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TypeCheckerFactory.getTypeChecker(unsupportedFormat),
                "Expected IllegalArgumentException for unsupported TableFormat");

        assertTrue(exception.getMessage().contains("No TypeChecker available for TableFormat"));
    }
}
