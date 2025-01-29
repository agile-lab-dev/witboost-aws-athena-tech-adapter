package com.witboost.provisioning.athena.service.provision;

import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.control.Either;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutputPortProvisionService implements ProvisionService {

    private final Logger logger = LoggerFactory.getLogger(OutputPortProvisionService.class);

    public OutputPortProvisionService() {}

    @Override
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {
        return Either.left(
                new FailedOperation("Method nod implemented", List.of(new Problem("Method not implemented"))));
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {
        return Either.left(
                new FailedOperation("Method nod implemented", List.of(new Problem("Method not implemented"))));
    }
}
