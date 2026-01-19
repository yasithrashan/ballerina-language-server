import ballerina/http;

// In-memory data storage
map<User> userStore = {};
map<Product> productStore = {};
int userIdCounter = 1;
int productIdCounter = 1;

// Utility Functions
function validateEmail(string email) returns boolean {
    return email.includes("@") && email.includes(".");
}

function validateAge(int age) returns boolean {
    return age > 0 && age < 150;
}

function calculateOrderTotal(decimal price, int quantity) returns decimal {
    return price * <decimal>quantity;
}

function getUserById(int userId) returns User? {
    return userStore[userId.toString()];
}

function getProductById(int productId) returns Product? {
    return productStore[productId.toString()];
}

// HTTP Listener
listener http:Listener httpListener = check new (8080);

// HTTP Service with Resources
service /api on httpListener {

    // Resource to get all users
    resource function get users() returns User[]|ErrorResponse {
        User[] users = userStore.toArray();
        return users;
    }

    // Resource to get a specific user by ID
    resource function get users/[int userId]() returns User|ErrorResponse {
        User? user = getUserById(userId);
        if user is User {
            return user;
        }
        return {message: "User not found", 'error: "NOT_FOUND"};
    }

    // Resource to create a new user
    resource function post users(@http:Payload User newUser) returns User|ErrorResponse {
        if !validateEmail(newUser.email) {
            return {message: "Invalid email format", 'error: "VALIDATION_ERROR"};
        }
        if !validateAge(newUser.age) {
            return {message: "Invalid age", 'error: "VALIDATION_ERROR"};
        }

        User user = {
            id: userIdCounter,
            name: newUser.name,
            email: newUser.email,
            age: newUser.age
        };
        userStore[userIdCounter.toString()] = user;
        userIdCounter = userIdCounter + 1;
        return user;
    }

    // Resource to update a user
    resource function put users/[int userId](@http:Payload User updatedUser) returns User|ErrorResponse {
        User? existingUser = getUserById(userId);
        if existingUser is () {
            return {message: "User not found", 'error: "NOT_FOUND"};
        }

        if !validateEmail(updatedUser.email) {
            return {message: "Invalid email format", 'error: "VALIDATION_ERROR"};
        }

        User user = {
            id: userId,
            name: updatedUser.name,
            email: updatedUser.email,
            age: updatedUser.age
        };
        userStore[userId.toString()] = user;
        return user;
    }

    // Resource to delete a user
    resource function delete users/[int userId]() returns http:Ok|ErrorResponse {
        User? user = getUserById(userId);
        if user is () {
            return {message: "User not found", 'error: "NOT_FOUND"};
        }
        _ = userStore.remove(userId.toString());
        return http:OK;
    }

    // Resource to get all products
    resource function get products() returns Product[]|ErrorResponse {
        Product[] products = productStore.toArray();
        return products;
    }

    // Resource to get a specific product by ID
    resource function get products/[int productId]() returns Product|ErrorResponse {
        Product? product = getProductById(productId);
        if product is Product {
            return product;
        }
        return {message: "Product not found", 'error: "NOT_FOUND"};
    }

    // Resource to create a new product
    resource function post products(@http:Payload Product newProduct) returns Product|ErrorResponse {
        if newProduct.price <= 0.0d {
            return {message: "Price must be greater than zero", 'error: "VALIDATION_ERROR"};
        }
        if newProduct.quantity < 0 {
            return {message: "Quantity cannot be negative", 'error: "VALIDATION_ERROR"};
        }

        Product product = {
            id: productIdCounter,
            name: newProduct.name,
            price: newProduct.price,
            quantity: newProduct.quantity
        };
        productStore[productIdCounter.toString()] = product;
        productIdCounter = productIdCounter + 1;
        return product;
    }

    // Resource to calculate order total
    resource function post orders/calculate(@http:Query int productId, @http:Query int quantity) returns Order|ErrorResponse {
        Product? product = getProductById(productId);
        if product is () {
            return {message: "Product not found", 'error: "NOT_FOUND"};
        }

        if quantity <= 0 {
            return {message: "Quantity must be greater than zero", 'error: "VALIDATION_ERROR"};
        }

        if product.quantity < quantity {
            return {message: "Insufficient stock", 'error: "INSUFFICIENT_STOCK"};
        }

        decimal totalPrice = calculateOrderTotal(product.price, quantity);

        Order 'order = {
            orderId: 0,
            userId: 0,
            productId: productId,
            quantity: quantity,
            totalPrice: totalPrice
        };

        return 'order;
    }

    // Resource to get service health status
    resource function get health() returns string {
        return "Service is running";
    }
}
