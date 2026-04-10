# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : main.bal

### Imports
- ballerina/grpc [L:17 - L:17]
- ballerina/log [L:18 - L:18]
- ballerinax/jaeger as _ [L:19 - L:19]
- wso2/client_stubs as stubs [L:20 - L:20]

### Configurables
- configurable datastore [L:26 - L:26]
- configurable redisHost [L:27 - L:27]
- configurable redisPassword [L:28 - L:28]

### Variables
- string greeting [L:22 - L:22]
- final string appName [L:23 - L:23]
- const int MAX_CONNECTIONS = 100 [L:24 - L:24]

### Services (Entry Points)
- service "CartService" on new grpc:Listener(9092) [L:30 - L:80]
    - description: Stores the product items added to the cart and retrieves them.
  - private final DataStore store [L:37 - L:37]
  - function init() returns error? [L:39 - L:47]
  - remote function AddItem(stubs:AddItemRequest : request) returns stubs:Empty|error [L:49 - L:58]
      - description: Adds an item to the cart.
  - remote function GetCart(stubs:GetCartRequest : request) returns stubs:Cart|error [L:60 - L:68]
      - description: Provides the cart with items.
  - remote function EmptyCart(stubs:EmptyCartRequest : request) returns stubs:Empty|error [L:70 - L:79]
      - description: Clears the cart.
