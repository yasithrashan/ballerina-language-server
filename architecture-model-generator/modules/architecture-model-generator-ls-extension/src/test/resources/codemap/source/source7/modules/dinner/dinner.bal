import ballerina/io;

public function getDinnerRequest(string userEmail) {
    io:println("Fetching dinner request for: " + userEmail);
}

public function upsertDinnerRequest(string userEmail, string mealOption, string date) {
    io:println("Dinner request saved for: " + userEmail + " | Meal: " + mealOption + " | Date: " + date);
}

public function cancelDinnerRequest(string userEmail) {
    io:println("Dinner request cancelled for: " + userEmail);
}
