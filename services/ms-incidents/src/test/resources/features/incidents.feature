@HU9 @HU10 @HU11 @HU12
Feature: Authenticated incident reporting and private evidence
  Scenario: A citizen creates a report owned by the authenticated identity
    When a citizen submits a valid incident
    Then the incident is active and owned by that citizen

  Scenario: A different citizen cannot resolve the report
    Given a citizen incident
    When another citizen resolves that incident
    Then the incident operation returns 403
    And the incident remains active

  Scenario: Evidence can only be downloaded by its owner
    Given a citizen incident with private evidence
    When another citizen downloads the evidence
    Then the incident operation returns 403
