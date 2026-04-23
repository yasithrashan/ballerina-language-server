# menu_app Codebase Summary

---

## File Path : main.bal

```ballerina
import ballerina/io [L:1 - L:1]
import wso2/menu_app.menu [L:2 - L:2]
import wso2/menu_app.dinner [L:3 - L:3]
```

```ballerina
public function main() [L:5 - L:16]
```

---

## File Path : modules/dinner/dinner.bal

```ballerina
import ballerina/io [L:1 - L:1]
```

```ballerina
public function getDinnerRequest(string userEmail) [L:3 - L:5]
public function upsertDinnerRequest(string userEmail, string mealOption, string date) [L:7 - L:9]
public function cancelDinnerRequest(string userEmail) [L:11 - L:13]
```

---

## File Path : modules/menu/menu.bal

```ballerina
import ballerina/io [L:1 - L:1]
```

```ballerina
public function getMenu() [L:3 - L:10]
public function addFeedback(string message) [L:12 - L:14]
```
