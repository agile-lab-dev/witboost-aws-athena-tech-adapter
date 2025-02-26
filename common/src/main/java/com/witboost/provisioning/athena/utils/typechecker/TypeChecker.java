package com.witboost.provisioning.athena.utils.typechecker;

import com.witboost.provisioning.athena.model.AthenaColumn;
import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;

public interface TypeChecker {
    Either<FailedOperation, String> resolveColumnType(AthenaColumn column);
}
