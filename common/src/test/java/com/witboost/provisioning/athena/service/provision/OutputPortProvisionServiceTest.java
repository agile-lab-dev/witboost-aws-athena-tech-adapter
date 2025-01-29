package com.witboost.provisioning.athena.service.provision;

import static org.mockito.Mockito.*;

import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class OutputPortProvisionServiceTest {

    @InjectMocks
    private OutputPortProvisionService outputPortProvisionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProvision_success() {
        Either<FailedOperation, ProvisionInfo> result =
                outputPortProvisionService.provision(mock(ProvisionOperationRequest.class));
        assert result.isLeft();
        assert result.getLeft().message().equals("Method nod implemented");
    }

    @Test
    void testUnrovision_success() {
        Either<FailedOperation, ProvisionInfo> result =
                outputPortProvisionService.unprovision(mock(ProvisionOperationRequest.class));
        assert result.isLeft();
        assert result.getLeft().message().equals("Method nod implemented");
    }
}
