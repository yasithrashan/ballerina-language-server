import ballerina/io;
import wso2/menu_app.menu;
import wso2/menu_app.dinner;

public function main() {
    io:println("=== Menu App ===");

    menu:getMenu();
    menu:addFeedback("Great lunch today!");

    io:println("=== Dinner Requests ===");

    dinner:getDinnerRequest("yasith@wso2.com");
    dinner:upsertDinnerRequest("yasith@wso2.com", "Veg Rice", "2026-04-20");
    dinner:cancelDinnerRequest("yasith@wso2.com");
}
