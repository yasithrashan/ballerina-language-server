# test Codebase Summary

---

## File Path : configurations.bal

```ballerina
configurable string DB_HOST [L:18 - L:18]
configurable int DB_PORT [L:19 - L:19]
configurable string DB_USER [L:20 - L:20]
configurable string DB_PASSWORD [L:21 - L:21]
configurable string DB_NAME [L:22 - L:22]
configurable string KAFKA_BROKER_URL [L:25 - L:25]
configurable string KAFKA_ORDER_EVENTS_TOPIC [L:26 - L:26]
configurable int SERVICE_PORT [L:29 - L:29]
```

---

## File Path : functions.bal

```ballerina
import ballerina/sql [L:16 - L:16]
import ballerina/time [L:17 - L:17]
import ballerina/uuid [L:18 - L:18]
import ballerina/log [L:19 - L:19]
import ballerina/lang.value as value [L:20 - L:20]
```

```ballerina
public function createNewOrder(OrderCreatePayload payload) returns OrderCreationResponse|error [L:25 - L:66]
public function getOrderById(string orderId) returns Order|OrderNotFoundError|error [L:69 - L:107]
function insertInitialOrder(string orderId, OrderCreatePayload payload, decimal totalAmount) returns sql:ExecutionResult|sql:Error [L:111 - L:133]
function publishOrderEvent(OrderCreatedEvent eventPayload) returns error? [L:137 - L:151]
function calculateTotal(OrderLinePayload[] lines) returns decimal [L:155 - L:161]
```

---

## File Path : main.bal

```ballerina
import ballerinax/postgresql [L:16 - L:16]
import ballerinax/kafka [L:17 - L:17]
import ballerina/log [L:18 - L:18]
import ballerina/lang.runtime as runtime [L:19 - L:19]
```

```ballerina
public final $CompilationError$ dbClient [L:23 - L:29]
public final $CompilationError$ kafkaProducer [L:32 - L:34]
```

```ballerina
public function main() [L:36 - L:48]
```

---

## File Path : service.bal

```ballerina
import ballerina/http [L:16 - L:16]
import ballerina/log [L:17 - L:17]
```

```ballerina
public type OrderCreationResponse record [L:61 - L:65]
```

```ballerina
@http:ServiceConfig {cors: {
        allowOrigins: ["https://grc.com"],
        allowMethods: ["GET", "POST"]
    }}
service /v1 on new http:Listener(SERVICE_PORT) { [L:20 - L:59]
    resource function post orders(@http:Payload OrderCreatePayload payload) returns OrderCreationResponse|http:InternalServerError|http:BadRequest [L:30 - L:47]
    resource function get orders/[string orderId]() returns Order|http:NotFound|http:InternalServerError [L:49 - L:58]
}
```

---

## File Path : types.bal

```ballerina
public type Order record [L:18 - L:30]
public type OrderModel record [L:33 - L:43]
public type OrderCreatePayload record [L:46 - L:53]
public type OrderLinePayload record [L:55 - L:58]
public type OrderLine record [L:60 - L:66]
public type Address record [L:68 - L:75]
public type Payment record [L:77 - L:81]
public type PaymentInfo record [L:83 - L:86]
public type Shipment record [L:88 - L:93]
public type OrderStatus "PENDING"|"CONFIRMED"|"AWAITING_PAYMENT"|"FULFILLING"|"SHIPPED"|"DELIVERED"|"CANCELLED"|"RETURNED"|"FAILED" [L:95 - L:95]
public type OrderCreatedEvent record [L:98 - L:103]
public type OrderCreatedEventData record [L:105 - L:112]
type OrderNotFoundError error [L:114 - L:114]
```
