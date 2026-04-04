# Project CodeMap

## CodeMap Structure

This document provides a structured overview of the project codebase.
It is organized by file path and summarizes the following elements for each file.
Each artifact is listed with its sub-properties on separate indented lines.


---

## File Path : agents.bal

---

## File Path : automation.bal

### Imports


- ballerina/log
  - **Line Range**: (0:0-0:21)

- ballerinax/googleapis.sheets as sheets
  - **Line Range**: (1:0-1:46)

### Variables


- columns
  - **Type**: SheetRow
  - **Line Range**: (4:0-4:119)

- currentTimeStamp
  - **Type**: string
  - **Line Range**: (5:0-5:63)

### Automations (Entry Points)


- public function main
  - **Parameters**: none
  - **Returns**: [error?]
  - **Line Range**: (7:0-74:1)

---

## File Path : config.bal

### Configurables


- configurable salesforceConfig
  - **Type**: record {|string refreshToken; string clientId; string clientSecret; string refreshUrl; string baseUrl; anydata...;|} & readonly
  - **Line Range**: (0:0-6:23)

- configurable googleConfig
  - **Type**: record {|string refreshToken; string clientId; string clientSecret; anydata...;|} & readonly
  - **Line Range**: (8:0-12:19)

- configurable timezone
  - **Type**: string
  - **Line Range**: (14:0-14:46)

- configurable spreadsheetId
  - **Type**: string? & readonly
  - **Line Range**: (15:0-15:40)

- configurable timeFrame
  - **Type**: TimeFrame & readonly
  - **Line Range**: (25:0-25:41)

### Types


- type TimeFrame
  - **Type Descriptor**: enum
  - **Fields**: [YESTERDAY, LAST_WEEK, LAST_MONTH, LAST_QUARTER, ALL]
  - **Line Range**: (17:0-23:2)

---

## File Path : connections.bal

### Imports


- ballerinax/salesforce
  - **Line Range**: (0:0-0:29)

- ballerinax/googleapis.sheets as sheets
  - **Line Range**: (1:0-1:46)

### Variables


- final salesforceClient
  - **Type**: salesforce:Client
  - **Line Range**: (3:0-11:3)

- final sheetsClient
  - **Type**: sheets:Client
  - **Line Range**: (13:0-20:3)

---

## File Path : data_mappings.bal

### Functions


- function mapOpportunityToRow
  - **Parameters**: [account: Opportunity]
  - **Returns**: [SheetRow]
  - **Line Range**: (1:0-12:2)

---

## File Path : functions.bal

### Imports


- ballerina/time
  - **Line Range**: (0:0-0:22)

### Functions


- function getFormattedCurrentTimeStamp
  - **Parameters**: none
  - **Returns**: [string|error]
  - **Line Range**: (2:0-10:1)

---

## File Path : main.bal

---

## File Path : types.bal

### Types


- type Attributes
  - **Type Descriptor**: record
  - **Fields**: ['type: string?, url: string?]
  - **Line Range**: (1:0-4:3)

- type Opportunity
  - **Type Descriptor**: record
  - **Fields**: [attributes: Attributes, Id: string?, AccountId: string?, OwnerId: string?, Name: string?, StageName: string?, Amount: decimal?, Probability: decimal?, ExpectedRevenue: decimal?, CloseDate: string?, Type: string?, LeadSource: string?, Description: string?, ContactId: string?, CampaignId: string?, Pricebook2Id: string?, NextStep: string?, TotalOpportunityQuantity: decimal?, IsClosed: boolean?, IsWon: boolean?, IsDeleted: boolean?, IsPrivate: boolean?, ForecastCategory: string?, ForecastCategoryName: string?, HasOpportunityLineItem: boolean?, HasOpenActivity: boolean?, HasOverdueTask: boolean?, PushCount: int?, FiscalYear: int?, FiscalQuarter: int?, Fiscal: string?, LastStageChangeDate: string?, LastActivityDate: string?, LastAmountChangedHistoryId: string?, LastCloseDateChangedHistoryId: string?, CreatedDate: string?, CreatedById: string?, LastModifiedDate: string?, LastModifiedById: string?, LastViewedDate: string?, LastReferencedDate: string?]
  - **Line Range**: (6:0-48:2)

- type SheetRow
  - **Type Descriptor**: (int|string|decimal|boolean|float)[]
  - **Line Range**: (50:0-50:51)
