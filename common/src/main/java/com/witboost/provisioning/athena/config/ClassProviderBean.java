package com.witboost.provisioning.athena.config;

import com.witboost.provisioning.athena.model.AthenaSpecific;
import com.witboost.provisioning.framework.service.ComponentClassProvider;
import com.witboost.provisioning.framework.service.SpecificClassProvider;
import com.witboost.provisioning.framework.service.impl.ComponentClassProviderImpl;
import com.witboost.provisioning.framework.service.impl.SpecificClassProviderImpl;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassProviderBean {

    @Bean
    public SpecificClassProvider specificClassProvider() {
        return SpecificClassProviderImpl.builder()
                .withDefaultSpecificClass(AthenaSpecific.class)
                .withDefaultReverseProvisionSpecificClass(Specific.class)
                .build();
    }

    @Bean
    public ComponentClassProvider componentClassProvider() {
        return ComponentClassProviderImpl.builder()
                .withDefaultClass(OutputPort.class)
                .build();
    }
}
