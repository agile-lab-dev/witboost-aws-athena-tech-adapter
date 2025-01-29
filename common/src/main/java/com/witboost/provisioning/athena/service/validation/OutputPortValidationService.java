package com.witboost.provisioning.athena.service.validation;

import com.witboost.provisioning.framework.service.validation.ComponentValidationService;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.OperationRequest;
import io.vavr.control.Either;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class OutputPortValidationService implements ComponentValidationService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortValidationService.class);

    public OutputPortValidationService() {}

    @Override
    public Either<FailedOperation, Void> validate(
            @Valid OperationRequest<?, ? extends Specific> operationRequest, OperationType operationType) {

        return Either.left(
                new FailedOperation("Method nod implemented", List.of(new Problem("Method not implemented"))));
    }
}
