# test Codebase Summary

---

## File Path : agents.bal

---

## File Path : automation.bal

```ballerina
import ballerina/log [L:1 - L:1]
import ballerinax/googleapis.sheets as sheets [L:2 - L:2]
```

```ballerina
SheetRow columns [L:5 - L:5]
string currentTimeStamp [L:6 - L:6]
```

```ballerina
public function main() returns error? [L:8 - L:75]
```

---

## File Path : config.bal

```ballerina
configurable salesforceConfig [L:1 - L:7]
configurable googleConfig [L:9 - L:13]
configurable timezone [L:15 - L:15]
configurable spreadsheetId [L:16 - L:16]
configurable timeFrame [L:26 - L:26]
```

```ballerina
type TimeFrame enum [L:18 - L:24]
```

---

## File Path : connections.bal

```ballerina
import ballerinax/salesforce [L:1 - L:1]
import ballerinax/googleapis.sheets as sheets [L:2 - L:2]
```

```ballerina
final salesforce:Client salesforceClient [L:4 - L:12]
final sheets:Client sheetsClient [L:14 - L:21]
```

---

## File Path : data_mappings.bal

```ballerina
function mapOpportunityToRow(Opportunity : account) returns SheetRow [L:2 - L:13]
```

---

## File Path : functions.bal

```ballerina
import ballerina/time [L:1 - L:1]
```

```ballerina
function getFormattedCurrentTimeStamp() returns string|error [L:3 - L:11]
```

---

## File Path : main.bal

---

## File Path : types.bal

```ballerina
type Attributes record [L:2 - L:5]
type Opportunity record [L:7 - L:49]
type SheetRow (int|string|decimal|boolean|float)[] [L:51 - L:51]
```
