package com.witboost.provisioning.athena.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AthenaSpecificTest {

    private Validator validator;
    private AthenaSpecific athenaSpecific1;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        athenaSpecific1 = createAthenaSpecific();
    }

    private AthenaSpecific createAthenaSpecific() {
        AthenaSpecific athenaSpecific = new AthenaSpecific();
        athenaSpecific.setStorageAreaId("area-001");

        AthenaTable athenaTable = new AthenaTable();
        athenaTable.setCatalog("catalog1");
        athenaTable.setDatabase("db1");
        athenaTable.setName("table1");
        athenaSpecific.setSourceTable(athenaTable);

        AthenaView athenaView = new AthenaView();
        athenaView.setCatalog("catalog2");
        athenaView.setDatabase("db2");
        athenaView.setName("view1");
        athenaSpecific.setView(athenaView);

        return athenaSpecific;
    }

    @Test
    public void testStorageAreaIdNotBlank() {
        athenaSpecific1.setStorageAreaId("");
        Set<ConstraintViolation<AthenaSpecific>> violations = validator.validate(athenaSpecific1);
        assertEquals(1, violations.size());
        assertEquals(
                "storageAreaId", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testSourceTableNotNull() {
        athenaSpecific1.setSourceTable(null);
        Set<ConstraintViolation<AthenaSpecific>> violations = validator.validate(athenaSpecific1);
        assertEquals(1, violations.size());
        assertEquals(
                "sourceTable", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testViewNotNull() {
        athenaSpecific1.setView(null);
        Set<ConstraintViolation<AthenaSpecific>> violations = validator.validate(athenaSpecific1);
        assertEquals(1, violations.size());
        assertEquals("view", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testStorageAreaIdNotNull() {
        athenaSpecific1.setStorageAreaId(null);
        Set<ConstraintViolation<AthenaSpecific>> violations = validator.validate(athenaSpecific1);
        assertEquals(1, violations.size());
        assertEquals(
                "storageAreaId", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testDefaultValues() {
        assertNotNull(athenaSpecific1);
        assertThat(athenaSpecific1.getStorageAreaId()).isNotBlank();
        assertNotNull(athenaSpecific1.getSourceTable());
        assertNotNull(athenaSpecific1.getView());
    }

    @Test
    public void testSettersAndGetters() {
        assertEquals("area-001", athenaSpecific1.getStorageAreaId());

        AthenaTable athenaTable = athenaSpecific1.getSourceTable();
        assertEquals("catalog1", athenaTable.getCatalog());
        assertEquals("db1", athenaTable.getDatabase());
        assertEquals("table1", athenaTable.getName());

        AthenaView athenaView = athenaSpecific1.getView();
        assertEquals("catalog2", athenaView.getCatalog());
        assertEquals("db2", athenaView.getDatabase());
        assertEquals("view1", athenaView.getName());
    }

    @AfterEach
    public void tearDown() {
        athenaSpecific1 = null;
    }
}
