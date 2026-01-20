import ballerina/io;

public function main() returns error? {
    io:println("gRPC Microservices Project Started!");
    io:println("=====================================");
    io:println("User Service running on port: 9091");
    io:println("Product Service running on port: 9092");
    io:println("=====================================");
    io:println("");
    io:println("Available Services:");
    io:println("1. UserService - Manages user operations");
    io:println("   - GetUser(user_id)");
    io:println("   - CreateUser(name, email)");
    io:println("   - ListUsers()");
    io:println("");
    io:println("2. ProductService - Manages product operations");
    io:println("   - GetProduct(product_id)");
    io:println("   - CreateProduct(name, description, price)");
    io:println("   - ListProducts()");
    io:println("");
    io:println("Services are ready to accept gRPC requests!");
}
