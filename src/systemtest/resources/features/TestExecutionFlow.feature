@TestExecutionFlowFeature
Feature: Test Execution system test

  @TestExecutionFlowScenario
  Scenario: Creates jenkins jobs, subscriptions and at last see that the last job has been
            triggered in order to know if all events has been aggregated correctly.

    Given configurations are provided

    #####Add TestExecution_Subscription_01_ActT to EI#####
    Given subscription object "TestExecution_Subscription_01_ActT" is created which will trigger "TestExecution_Job_02_ActF"
    When notification with key "EVENT_ID" and value "activity_triggered_event_id" is added to "TestExecution_Subscription_01_ActT"
    And condition with jmespath "activity_started_event_id == null" is added to "TestExecution_Subscription_01_ActT"
    Then we send the "TestExecution_Subscription_01_ActT" to eiffel intelligence for creation.

    #####Add TestExecution_Subscription_02_ActF to EI#####
    Given subscription object "TestExecution_Subscription_02_ActF" is created which will trigger "TestExecution_Job_03_TERCC"
    When notification with key "EVENT_ID" and value "activity_triggered_event_id" is added to "TestExecution_Subscription_02_ActF"
    And condition with jmespath "activity_finished_event_id != null" is added to "TestExecution_Subscription_02_ActF"
    And condition with jmespath "test_batches == null" is added to "TestExecution_Subscription_02_ActF"
    Then we send the "TestExecution_Subscription_02_ActF" to eiffel intelligence for creation.

    #####Add TestExecution_Subscription_03_TERCC to EI#####
    Given subscription object "TestExecution_Subscription_03_TERCC" is created which will trigger "TestExecution_Job_04_TSS"
    When notification with key "EVENT_ID" and value "activity_triggered_event_id" is added to "TestExecution_Subscription_03_TERCC"
    And condition with jmespath "test_batches != null" is added to "TestExecution_Subscription_03_TERCC"
    And condition with jmespath "test_suite == null" is added to "TestExecution_Subscription_03_TERCC"
    Then we send the "TestExecution_Subscription_03_TERCC" to eiffel intelligence for creation.

    #####Add TestExecution_Subscription_04_TSS to EI#####
    Given subscription object "TestExecution_Subscription_04_TSS" is created which will trigger "TestExecution_Job_05_TCT"
    When notification with key "EVENT_ID" and value "test_suite[0].test_suite_started_event_id" is added to "TestExecution_Subscription_04_TSS"
    And condition with jmespath "test_suite != null" is added to "TestExecution_Subscription_04_TSS"
    And condition with jmespath "test_suite[0].test_case == null" is added to "TestExecution_Subscription_04_TSS"
    Then we send the "TestExecution_Subscription_04_TSS" to eiffel intelligence for creation.

    #####Add TestExecution_Subscription_05_TCT to EI#####
    Given subscription object "TestExecution_Subscription_05_TCT" is created which will trigger "TestExecution_Job_06_TCF"
    When notification with key "EVENT_ID_CASE1" and value "test_suite[0].test_case[0].test_case_triggered_event_id" is added to "TestExecution_Subscription_05_TCT"
    When notification with key "EVENT_ID_CASE2" and value "test_suite[0].test_case[1].test_case_triggered_event_id" is added to "TestExecution_Subscription_05_TCT"
    And condition with jmespath "test_suite[0].test_case[1] != null" is added to "TestExecution_Subscription_05_TCT"
    And condition with jmespath "test_suite[0].test_case[1].test_case_finished_event_id == null" is added to "TestExecution_Subscription_05_TCT"
    Then we send the "TestExecution_Subscription_05_TCT" to eiffel intelligence for creation.

    #####Add TestExecution_Subscription_06_TCF to EI#####
    Given subscription object "TestExecution_Subscription_06_TCF" is created which will trigger "TestExecution_Job_07_Complete"
    When notification with key "TEST_CONCLUSION" and value "test_suite[0].test_case[0].conclusion" is added to "TestExecution_Subscription_06_TCF"
    And condition with jmespath "test_suite[0].test_case[0].conclusion == 'SUCCESSFUL'" is added to "TestExecution_Subscription_06_TCF"
    And condition with jmespath "test_suite[0].test_case[1].conclusion == 'SUCCESSFUL'" is added to "TestExecution_Subscription_06_TCF"
    Then we send the "TestExecution_Subscription_06_TCF" to eiffel intelligence for creation.