import ballerina/io;

public function getMenu() {
    io:println("Date: 2026-04-20");
    io:println("Breakfast: Egg sandwich");
    io:println("Juice: Orange juice");
    io:println("Lunch: Rice and curry");
    io:println("Dessert: Fruit salad");
    io:println("Snack: Biscuits");
}

public function addFeedback(string message) {
    io:println("Feedback added: " + message);
}
