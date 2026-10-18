package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.*;

public interface WarehouseService {

    WarehouseReceiptResponse receive(
            WarehouseReceiveRequest request);

    FreightAssignmentResponse assignLocation(
            AssignLocationRequest request);

    DispatchResponse dispatch(
            DispatchRequest request);
}
