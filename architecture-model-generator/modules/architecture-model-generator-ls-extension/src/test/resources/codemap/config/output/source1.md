# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : configurations.bal

### Configurables


- configurable DB_HOST
  - **Type**: string
  - **Line Range**: (17:0-17:42)

- configurable DB_PORT
  - **Type**: int
  - **Line Range**: (18:0-18:32)

- configurable DB_USER
  - **Type**: string
  - **Line Range**: (19:0-19:37)

- configurable DB_PASSWORD
  - **Type**: string
  - **Line Range**: (20:0-20:45)

- configurable DB_NAME
  - **Type**: string
  - **Line Range**: (21:0-21:41)

- configurable KAFKA_BROKER_URL
  - **Type**: string
  - **Line Range**: (24:0-24:56)

- configurable KAFKA_ORDER_EVENTS_TOPIC
  - **Type**: string
  - **Line Range**: (25:0-25:62)

- configurable SERVICE_PORT
  - **Type**: int
  - **Line Range**: (28:0-28:37)

---

## File Path : functions.bal

### Imports


- ballerina/sql
  - **Line Range**: (15:0-15:21)

- ballerina/time
  - **Line Range**: (16:0-16:22)

- ballerina/uuid
  - **Line Range**: (17:0-17:22)

- ballerina/log
  - **Line Range**: (18:0-18:21)

- ballerina/lang.value as value
  - **Line Range**: (19:0-19:37)

### Functions


- public function createNewOrder
  - **Parameters**: [payload: OrderCreatePayload]
  - **Returns**: [OrderCreationResponse|error]
  - **Line Range**: (24:0-65:1)

- public function getOrderById
  - **Parameters**: [orderId: string]
  - **Returns**: [Order|OrderNotFoundError|error]
  - **Line Range**: (68:0-106:1)

- function insertInitialOrder
  - **Parameters**: [orderId: string, payload: OrderCreatePayload, totalAmount: decimal]
  - **Returns**: [sql:ExecutionResult|sql:Error]
  - **Line Range**: (110:0-132:1)

- function publishOrderEvent
  - **Parameters**: [eventPayload: OrderCreatedEvent]
  - **Returns**: [error?]
  - **Line Range**: (136:0-150:1)

- function calculateTotal
  - **Parameters**: [lines: OrderLinePayload[]]
  - **Returns**: [decimal]
  - **Line Range**: (154:0-160:1)

---

## File Path : main.bal

### Imports


- ballerinax/postgresql
  - **Line Range**: (15:0-15:29)

- ballerinax/kafka
  - **Line Range**: (16:0-16:24)

- ballerina/log
  - **Line Range**: (17:0-17:21)

- ballerina/lang.runtime as runtime
  - **Line Range**: (18:0-18:41)

### Variables


- final dbClient
  - **Type**: postgresql:Client
  - **Line Range**: (22:0-28:2)

- final kafkaProducer
  - **Type**: kafka:Producer
  - **Line Range**: (31:0-33:2)

### Automations (Entry Points)


- public function main
  - **Parameters**: none
  - **Returns**: ()
  - **Line Range**: (35:0-47:1)

---

## File Path : service.bal

### Imports


- ballerina/http
  - **Line Range**: (15:0-15:22)

- ballerina/log
  - **Line Range**: (16:0-16:21)

### Types


- type OrderCreationResponse
  - **Type Descriptor**: record
  - **Fields**: [orderId: string, status: string, message: string]
  - **Line Range**: (60:0-64:3)

### Services (Entry Points)


- service /v1
  - **Base Path**: /v1
  - **Listener Type**: http:Listener
  - **Line Range**: (19:0-58:1)

  - post resource function orders
    - **Parameters**: [payload: OrderCreatePayload]
    - **Returns**: [OrderCreationResponse|http:InternalServerError|http:BadRequest]
    - **Line Range**: (29:4-46:5)

  - get resource function orders/[string orderId]
    - **Parameters**: none
    - **Returns**: [Order|http:NotFound|http:InternalServerError]
    - **Line Range**: (48:4-57:5)

---

## File Path : types.bal

### Types


- type Order
  - **Type Descriptor**: record
  - **Fields**: [orderId: string, customerId: string, status: OrderStatus, createdAt: string, totalAmount: decimal, currency: string, shippingAddress: Address, billingAddress: Address, orderLines: OrderLine[], payments: Payment[], shipments: Shipment[]]
  - **Line Range**: (17:0-29:2)

- type OrderModel
  - **Type Descriptor**: record
  - **Fields**: [orderId: string, customerId: string, status: OrderStatus, createdAt: string, totalAmount: decimal, currency: string, shippingAddress: json, billingAddress: json, orderLines: json]
  - **Line Range**: (32:0-42:3)

- type OrderCreatePayload
  - **Type Descriptor**: record
  - **Fields**: [customerId: string, currency: string, shippingAddress: Address, billingAddress: Address, orderLines: OrderLinePayload[], paymentInfo: PaymentInfo]
  - **Line Range**: (45:0-52:3)

- type OrderLinePayload
  - **Type Descriptor**: record
  - **Fields**: [sku: string, quantity: int]
  - **Line Range**: (54:0-57:3)

- type OrderLine
  - **Type Descriptor**: record
  - **Fields**: [lineId: string, sku: string, quantity: int, unitPrice: decimal, lineTotal: decimal]
  - **Line Range**: (59:0-65:3)

- type Address
  - **Type Descriptor**: record
  - **Fields**: [line1: string, line2: string?, city: string, state: string, zipCode: string, country: string]
  - **Line Range**: (67:0-74:3)

- type Payment
  - **Type Descriptor**: record
  - **Fields**: [paymentId: string, status: string, amount: decimal]
  - **Line Range**: (76:0-80:3)

- type PaymentInfo
  - **Type Descriptor**: record
  - **Fields**: [paymentMethodToken: string, amount: decimal]
  - **Line Range**: (82:0-85:3)

- type Shipment
  - **Type Descriptor**: record
  - **Fields**: [shipmentId: string, trackingNumber: string, carrier: string, status: string]
  - **Line Range**: (87:0-92:3)

- type OrderStatus
  - **Type Descriptor**: "PENDING"|"CONFIRMED"|"AWAITING_PAYMENT"|"FULFILLING"|"SHIPPED"|"DELIVERED"|"CANCELLED"|"RETURNED"|"FAILED"
  - **Line Range**: (94:0-94:132)

- type OrderCreatedEvent
  - **Type Descriptor**: record
  - **Fields**: [eventId: string, eventType: string, timestamp: string, data: OrderCreatedEventData]
  - **Line Range**: (97:0-102:3)

- type OrderCreatedEventData
  - **Type Descriptor**: record
  - **Fields**: [orderId: string, customerId: string, currency: string, totalAmount: decimal, orderLines: OrderLinePayload[], paymentInfo: PaymentInfo]
  - **Line Range**: (104:0-111:3)

- type OrderNotFoundError
  - **Type Descriptor**: error
  - **Line Range**: (113:0-113:30)
