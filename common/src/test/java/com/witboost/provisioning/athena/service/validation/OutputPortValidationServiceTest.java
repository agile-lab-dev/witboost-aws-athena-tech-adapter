package com.witboost.provisioning.athena.service.validation;

import static org.mockito.Mockito.mock;

import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

class OutputPortValidationServiceTest {

    @Test
    void validate() {
        var validationService = new OutputPortValidationService();

        Either<FailedOperation, ProvisionInfo> result =
                validationService.validate(mock(ProvisionOperationRequest.class), OperationType.VALIDATE);
        assert (result.isLeft());
        assert result.getLeft().message().equals("Method nod implemented");
    }
}
