# LogiCore OMS API Examples

Base URL:

```text
http://localhost:8080
```

## 1. Create Shipment

```http
POST /api/shipments
Content-Type: application/json

{
  "customerName": "Northstar Retail",
  "origin": "Chennai",
  "destination": "Seattle",
  "mode": "FREIGHT",
  "weight": 125.50
}
```

## 2. Receive Freight At Warehouse

```http
POST /api/warehouse/receive
Content-Type: application/json

{
  "shipmentId": 1,
  "condition": "GOOD",
  "weightChecked": 125.50,
  "receivedBy": "warehouse-user"
}
```

## 3. Assign Storage Location

```http
POST /api/warehouse/assign-location
Content-Type: application/json

{
  "shipmentId": 1,
  "zone": "A",
  "rack": "R1",
  "bin": "B4",
  "assignedBy": "warehouse-user"
}
```

## 4. Dispatch Shipment

```http
POST /api/dispatch
Content-Type: application/json

{
  "shipmentId": 1,
  "vehicleNumber": "TN-10-AB-2026",
  "destination": "Seattle Delivery Hub",
  "dispatchedBy": "dispatch-user"
}
```

## 5. Track Shipment

```http
GET /api/tracking/TRK-2026-00001
```

The tracking response is customer-safe. It does not expose warehouse staff names, storage rack/bin, or internal operational remarks.

## 6. Cancel Shipment Before Transit

```http
DELETE /api/shipments/1
Content-Type: application/json

{
  "reason": "Customer requested cancellation before dispatch",
  "cancelledBy": "operations-user"
}
```
