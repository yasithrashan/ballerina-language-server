// Type Definitions
type User record {|
    int id;
    string name;
    string email;
    int age;
|};

type Product record {|
    int id;
    string name;
    decimal price;
    int quantity;
|};

type Order record {|
    int orderId;
    int userId;
    int productId;
    int quantity;
    decimal totalPrice;
|};

type ErrorResponse record {|
    string message;
    string 'error;
|};
