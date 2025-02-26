package com.witboost.provisioning.athena.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.athena.model.AthenaOutputPort;
import com.witboost.provisioning.athena.model.AthenaSpecific;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

class ClassProviderBeanTest {

    ClassProviderBean classProviderBean = new ClassProviderBean();

    @Test
    void defaultSpecificProvider() {
        var specificProvider = classProviderBean.specificClassProvider();
        assertEquals(Option.of(AthenaSpecific.class), specificProvider.get("a-urn"));
    }

    @Test
    void defaultComponentProvider() {
        var componentProvider = classProviderBean.componentClassProvider();
        assertEquals(Option.of(AthenaOutputPort.class), componentProvider.get("whatever"));
    }
}
