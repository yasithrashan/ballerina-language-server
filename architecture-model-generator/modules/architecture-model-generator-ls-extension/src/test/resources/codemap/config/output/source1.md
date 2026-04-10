# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : configurations.bal

### Configurables
- configurable DB_HOST [L:18 - L:18]
- configurable DB_PORT [L:19 - L:19]
- configurable DB_USER [L:20 - L:20]
- configurable DB_PASSWORD [L:21 - L:21]
- configurable DB_NAME [L:22 - L:22]
- configurable KAFKA_BROKER_URL [L:25 - L:25]
- configurable KAFKA_ORDER_EVENTS_TOPIC [L:26 - L:26]
- configurable SERVICE_PORT [L:29 - L:29]

---

## File Path : functions.bal

### Imports
- ballerina/sql [L:16 - L:16]
- ballerina/time [L:17 - L:17]
- ballerina/uuid [L:18 - L:18]
- ballerina/log [L:19 - L:19]
- ballerina/lang.value as value [L:20 - L:20]

### Functions
- public function createNewOrder(OrderCreatePayload : payload) returns OrderCreationResponse|error [L:25 - L:66]
- public function getOrderById(string : orderId) returns Order|OrderNotFoundError|error [L:69 - L:107]
- function insertInitialOrder(string : orderId, OrderCreatePayload : payload, decimal : totalAmount) returns sql:ExecutionResult|sql:Error [L:111 - L:133]
- function publishOrderEvent(OrderCreatedEvent : eventPayload) returns error? [L:137 - L:151]
- function calculateTotal(OrderLinePayload[] : lines) returns decimal [L:155 - L:161]

---

## File Path : main.bal

### Imports
- ballerinax/postgresql [L:16 - L:16]
- ballerinax/kafka [L:17 - L:17]
- ballerina/log [L:18 - L:18]
- ballerina/lang.runtime as runtime [L:19 - L:19]

### Variables
- final postgresql:Client dbClient [L:23 - L:29]
- final kafka:Producer kafkaProducer [L:32 - L:34]

### Automations (Entry Points)
- public function main() [L:36 - L:48]

---

## File Path : service.bal

### Imports
- ballerina/http [L:16 - L:16]
- ballerina/log [L:17 - L:17]

### Types
- type OrderCreationResponse record [L:61 - L:65]

### Services (Entry Points)
- service /v1 on new http:Listener(SERVICE_PORT) [L:20 - L:59]
  - resource function post orders(OrderCreatePayload : payload) returns OrderCreationResponse|http:InternalServerError|http:BadRequest [L:30 - L:47]
  - resource function get orders/[string orderId]() returns Order|http:NotFound|http:InternalServerError [L:49 - L:58]

---

## File Path : types.bal

### Types
- type Order record [L:18 - L:30]
- type OrderModel record [L:33 - L:43]
- type OrderCreatePayload record [L:46 - L:53]
- type OrderLinePayload record [L:55 - L:58]
- type OrderLine record [L:60 - L:66]
- type Address record [L:68 - L:75]
- type Payment record [L:77 - L:81]
- type PaymentInfo record [L:83 - L:86]
- type Shipment record [L:88 - L:93]
- type OrderStatus "PENDING"|"CONFIRMED"|"AWAITING_PAYMENT"|"FULFILLING"|"SHIPPED"|"DELIVERED"|"CANCELLED"|"RETURNED"|"FAILED" [L:95 - L:95]
- type OrderCreatedEvent record [L:98 - L:103]
- type OrderCreatedEventData record [L:105 - L:112]
- type OrderNotFoundError error [L:114 - L:114]
