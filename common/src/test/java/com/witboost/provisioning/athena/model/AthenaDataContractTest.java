package com.witboost.provisioning.athena.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AthenaDataContractTest {

    @Test
    void testDefaultInitialization() {
        AthenaDataContract contract = new AthenaDataContract();

        assertTrue(contract.getTermsAndConditions().isEmpty());
        assertTrue(contract.getSLA().isEmpty());
        assertTrue(contract.getEndpoint().isEmpty());
        assertTrue(contract.getBiTempBusinessTs().isEmpty());
        assertTrue(contract.getBiTempWriteTs().isEmpty());

        assertNull(contract.getSchema());
        assertNull(contract.getAdditionalProperties());
    }

    @Test
    void testSettersAndGetters() {
        AthenaDataContract contract = new AthenaDataContract();

        contract.setTermsAndConditions(Optional.of("Some terms"));
        contract.setEndpoint(Optional.of("http://example.com"));
        contract.setBiTempBusinessTs(Optional.of("2023-01-01T00:00:00Z"));

        assertEquals("Some terms", contract.getTermsAndConditions().get());
        assertEquals("http://example.com", contract.getEndpoint().get());
        assertEquals("2023-01-01T00:00:00Z", contract.getBiTempBusinessTs().get());
    }

    @Test
    void testJsonSerializationAndDeserialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        // JSON di input
        String json =
                """
            {
                "schema": [
                    {
                        "name": "column1",
                        "description": "description1",
                        "dataType": "STRING"
                    }
                ],
                "termsAndConditions": "Some terms",
                "endpoint": "http://example.com",
                "biTempBusinessTs": "2023-01-01T00:00:00Z",
                "additionalProperty1": "value1",
                "additionalProperty2": "value2"
            }
            """;

        AthenaDataContract contract = objectMapper.readValue(json, AthenaDataContract.class);

        assertNotNull(contract.getSchema());
        assertEquals(1, contract.getSchema().size());
        assertEquals("column1", contract.getSchema().get(0).getName());
        assertEquals("description1", contract.getSchema().get(0).getDescription());
        assertEquals("STRING", contract.getSchema().get(0).getDataType());

        assertEquals("Some terms", contract.getTermsAndConditions().get());
        assertEquals("http://example.com", contract.getEndpoint().get());
        assertEquals("2023-01-01T00:00:00Z", contract.getBiTempBusinessTs().get());

        Map<String, JsonNode> additionalProperties = contract.getAdditionalProperties();
        assertNotNull(additionalProperties);
        assertEquals("value1", additionalProperties.get("additionalProperty1").asText());
        assertEquals("value2", additionalProperties.get("additionalProperty2").asText());
    }

    @Test
    void testJsonSerialization() throws JsonProcessingException {
        AthenaDataContract contract = new AthenaDataContract();
        contract.setTermsAndConditions(Optional.of("Some terms"));
        contract.setEndpoint(Optional.of("http://example.com"));
        contract.setBiTempBusinessTs(Optional.of("2023-01-01T00:00:00Z"));

        AthenaColumn column = new AthenaColumn();
        column.setName("column1");
        column.setDescription("description1");
        column.setDataType("STRING");
        contract.setSchema(List.of(column));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());

        contract.setAdditionalProperties(Map.of(
                "additionalProperty1", objectMapper.readTree("\"value1\""),
                "additionalProperty2", objectMapper.readTree("\"value2\"")));

        String json = objectMapper.writeValueAsString(contract);

        assertTrue(json.contains("\"termsAndConditions\":\"Some terms\""));
        assertTrue(json.contains("\"endpoint\":\"http://example.com\""));
        assertTrue(json.contains("\"biTempBusinessTs\":\"2023-01-01T00:00:00Z\""));
        assertTrue(json.contains("\"name\":\"column1\""));
        assertTrue(json.contains("\"additionalProperty1\":\"value1\""));
        assertTrue(json.contains("\"additionalProperty2\":\"value2\""));
    }
}
