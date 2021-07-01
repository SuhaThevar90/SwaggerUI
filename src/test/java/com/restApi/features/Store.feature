#Data Table having keywords GenerateNumber/GenerateString is then updated with dynamic values in stepdefinition
#For Pets request json, sample json is used as template however for Store and User jsons dynamic json gets created

Feature: Verify all Store Operations

Scenario: Verify Create Store operation
    Given I create a new store order with url "/store/order" and orderId variable
    |id        |GenerateNumber |
    | petId    | GenerateNumber |
    | quantity | 2 |
    |shipDate  | CurrentDate |
    | status   | approved |
    | complete | true | 
    Then I perform get operation to fetch order details for url "store/order/" with orderId variable
     And I verify response details
 
 Scenario: Verify Invalid ID supplied Scenario
    When I perform get operation to fetch order details for url "store/order/" with orderId value a
    Then I verify response has "Input error" with status code 400
  # 
 Scenario: Verify Order Not Found Scenario
    When I perform get operation to fetch order details for url "store/order/" with orderId value 6
    Then I verify response has "Order not found" with status code 404
  # 
 Scenario: Verify get pet Inventories by status Scenario
    When I perform get operation to fetch inventory details for url "/store/inventory"
    Then I verify response received has "approved|delivered|placed"
    
     Scenario: Verify Delete Order operation
     Given I create a new store order with url "/store/order" and orderId variable
     |id        |GenerateNumber |
    | petId    | GenerateNumber |
    | quantity | 2 |
    |shipDate  | CurrentDate |
    | status   | approved |
    | complete | true |
    When I perform delete operation to delete order details for url "/store/order/" with orderId variable
    Then I verify response has "" with status code 200
   
  # 
 