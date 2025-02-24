package com.witboost.provisioning.athena.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.witboost.provisioning.athena.model.AthenaSpecific;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.OperationRequest;
import io.vavr.control.Either;
import io.vavr.control.Option;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    public static Either<FailedOperation, OutputPort<? extends Specific>> getOutputPort(
            OperationRequest<?, ? extends Specific> operationRequest) {

        var component = operationRequest.getComponent();
        if (component.isEmpty()) {
            String error =
                    String.format("Invalid operation request: Component is missing. Request: %s", operationRequest);
            logger.error(error);
            return Either.left(new FailedOperation(error, List.of(new Problem(error))));
        }

        if (component.get().getKind().equalsIgnoreCase("outputport")
                && component.get() instanceof OutputPort<? extends Specific> op) return Either.right(op);

        String error = String.format(
                "Invalid operation request: Component %s is not an OutputPort. Request: %s",
                component.get().getName(), operationRequest);
        logger.error(error);
        return Either.left(new FailedOperation(error, List.of(new Problem(error))));
    }

    public static Either<FailedOperation, AthenaSpecific> getAthenaSpecific(
            com.witboost.provisioning.model.Component<? extends Specific> component) {

        var componentSpecific = component.getSpecific();

        if (componentSpecific instanceof @Valid AthenaSpecific) return Either.right((AthenaSpecific) componentSpecific);

        String error = String.format("Invalid Specific type of %s. Expected AthenaSpecific.", component.getName());
        logger.error(error);
        return Either.left(new FailedOperation(error, List.of(new Problem(error))));
    }

    public static Either<FailedOperation, JsonNode> extractStorageAreaInfo(
            OperationRequest<?, ? extends Specific> operationRequest, AthenaSpecific athenaSpecific) {
        Option<JsonNode> privateInfoJN =
                operationRequest.getDataProduct().getComponentToProvision(athenaSpecific.getStorageAreaId());

        if (privateInfoJN.isEmpty()) {
            return Either.left(new FailedOperation(
                    "Could not extract the dependant storage component",
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new Problem(
                            "The specific.storageAreaId field does not match any component in the descriptor"))));
        }

        return Either.right(privateInfoJN.get());
    }

    public static Either<FailedOperation, String> extractStorageAreaRegion(JsonNode storageAreaInfo) {

        return Option.of(storageAreaInfo.get("specific"))
                .flatMap(specific -> Option.of(specific.get("region")))
                .map(JsonNode::asText)
                .toEither(() -> new FailedOperation(
                        "The dependent storage component does not include the AWS region",
                        Optional.empty(),
                        Optional.empty(),
                        List.of(new Problem("Missing AWS region at info.specific.region.value"))));
    }
}
