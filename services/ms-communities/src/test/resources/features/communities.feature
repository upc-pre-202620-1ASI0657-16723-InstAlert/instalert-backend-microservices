@HU24 @HU26 @HU29 @HU30
Feature: Community membership and explicit monitoring consent
  Scenario: Private community rejects an unrelated citizen
    Given a private community owned by a citizen
    When another citizen reads the private community
    Then the community operation returns 403

  Scenario: Only the invited recipient can accept a trust link
    Given a pending trust invitation between two citizens
    When the requester accepts their own invitation
    Then the community operation returns 403
    When the recipient accepts the invitation
    Then the community operation returns 204

  Scenario: Revoked monitoring permission no longer authorizes access
    Given an accepted trust link with monitoring permission
    When the owner revokes monitoring permission
    Then the community operation returns 204
    When the recipient queries monitoring after revocation
    Then the community operation returns 403
