# Project CodeMap Structure

---

## File: main.bal : main.bal

### VARIABLE:
- name: userStore
- type: map<yasithrashan/bal_ls_test:0.1.0:User>
- lineRange: fileName: main.bal, startLine: {line: 3, offset: 0}, endLine: {line: 3, offset: 25}

### VARIABLE:
- name: productStore
- type: map<yasithrashan/bal_ls_test:0.1.0:Product>
- lineRange: fileName: main.bal, startLine: {line: 4, offset: 0}, endLine: {line: 4, offset: 31}

### VARIABLE:
- name: userIdCounter
- type: int
- lineRange: fileName: main.bal, startLine: {line: 5, offset: 0}, endLine: {line: 5, offset: 22}

### VARIABLE:
- name: productIdCounter
- type: int
- lineRange: fileName: main.bal, startLine: {line: 6, offset: 0}, endLine: {line: 6, offset: 25}

### FUNCTION:
- name: validateEmail
- modifiers: []
- lineRange: fileName: main.bal, startLine: {line: 9, offset: 0}, endLine: {line: 11, offset: 1}
- parameters: [email: string]
- returns: boolean

### FUNCTION:
- name: validateAge
- modifiers: []
- lineRange: fileName: main.bal, startLine: {line: 13, offset: 0}, endLine: {line: 15, offset: 1}
- parameters: [age: int]
- returns: boolean

### FUNCTION:
- name: calculateOrderTotal
- modifiers: []
- lineRange: fileName: main.bal, startLine: {line: 17, offset: 0}, endLine: {line: 19, offset: 1}
- parameters: [price: decimal, quantity: int]
- returns: decimal

### FUNCTION:
- name: getUserById
- modifiers: []
- lineRange: fileName: main.bal, startLine: {line: 21, offset: 0}, endLine: {line: 23, offset: 1}
- parameters: [userId: int]
- returns: User?

### FUNCTION:
- name: getProductById
- modifiers: []
- lineRange: fileName: main.bal, startLine: {line: 25, offset: 0}, endLine: {line: 27, offset: 1}
- parameters: [productId: int]
- returns: Product?

### LISTENER:
- name: httpListener
- line: 30
- type: ballerina/http:2.15.4:Listener
- port: 
- config: 
- modifiers: []

### SERVICE(ENTRY_POINT):
- basePath: /api
- port: 
- lineRange: fileName: main.bal, startLine: {line: 33, offset: 0}, endLine: {line: 168, offset: 1}

#### RESOURCE_FUNCTIONS:

##### RESOURCE:
- name: users
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 36, offset: 4}, endLine: {line: 39, offset: 5}
- accessor: get
- parameters: []
- returns: User[]|ErrorResponse

##### RESOURCE:
- name: users/[int userId]
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 42, offset: 4}, endLine: {line: 48, offset: 5}
- accessor: get
- parameters: []
- returns: User|ErrorResponse

##### RESOURCE:
- name: users
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 51, offset: 4}, endLine: {line: 68, offset: 5}
- accessor: post
- parameters: [newUser: User]
- returns: User|ErrorResponse

##### RESOURCE:
- name: users/[int userId]
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 71, offset: 4}, endLine: {line: 89, offset: 5}
- accessor: put
- parameters: [updatedUser: User]
- returns: User|ErrorResponse

##### RESOURCE:
- name: users/[int userId]
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 92, offset: 4}, endLine: {line: 99, offset: 5}
- accessor: delete
- parameters: []
- returns: http:Ok|ErrorResponse

##### RESOURCE:
- name: products
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 102, offset: 4}, endLine: {line: 105, offset: 5}
- accessor: get
- parameters: []
- returns: Product[]|ErrorResponse

##### RESOURCE:
- name: products/[int productId]
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 108, offset: 4}, endLine: {line: 114, offset: 5}
- accessor: get
- parameters: []
- returns: Product|ErrorResponse

##### RESOURCE:
- name: products
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 117, offset: 4}, endLine: {line: 134, offset: 5}
- accessor: post
- parameters: [newProduct: Product]
- returns: Product|ErrorResponse

##### RESOURCE:
- name: orders/calculate
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 137, offset: 4}, endLine: {line: 162, offset: 5}
- accessor: post
- parameters: [productId: int, quantity: int]
- returns: Order|ErrorResponse

##### RESOURCE:
- name: health
- modifiers: [resource]
- lineRange: fileName: main.bal, startLine: {line: 165, offset: 4}, endLine: {line: 167, offset: 5}
- accessor: get
- parameters: []
- returns: string

---

## File: types.bal : types.bal

### TYPE:
- name: User
- lineRange: fileName: types.bal, startLine: {line: 1, offset: 0}, endLine: {line: 6, offset: 3}
- fields: []

### TYPE:
- name: Product
- lineRange: fileName: types.bal, startLine: {line: 8, offset: 0}, endLine: {line: 13, offset: 3}
- fields: []

### TYPE:
- name: Order
- lineRange: fileName: types.bal, startLine: {line: 15, offset: 0}, endLine: {line: 21, offset: 3}
- fields: []

### TYPE:
- name: ErrorResponse
- lineRange: fileName: types.bal, startLine: {line: 23, offset: 0}, endLine: {line: 26, offset: 3}
- fields: []

---