# Backend continuous integration

`Backend CI` runs on pushes to main, develop, feature, release and hotfix branches,
on pull requests into main/develop, and manually with workflow_dispatch.

Five independent Ubuntu jobs use Java 21 and the repository Maven wrapper to run
`verify` for Account, Emergencies, Incidents, Communities and API Gateway. The
existing tests include JUnit/MockMvc, Cucumber and H2/Flyway persistence checks.
Each successful test/package step is followed by a build of that service's Docker
image. JARs and Maven test reports are retained as artifacts for 14 days, including
available reports after failures.

No production credentials are required. Docker images are built on the runner;
they are not pushed to a registry or deployed. This workflow does not start the
complete MySQL/Compose environment or run the separate 29 live checks/Postman
collection. Those remain distinct local deployment evidence. The legacy monolith
is outside this workflow's five extracted components.

The workflow was introduced after the v0.3.0 integration, during Sprint 3 evidence
preparation. It must not be described as CI executed during Sprints 1 or 2.

For evidence, open Actions > Backend CI > a successful run and capture its branch,
commit, result and five jobs. Open the Incidents/Communities Test and package logs
for test totals, and Build Docker image for image-build evidence. Record the run
URL and actual execution date. An unexecuted workflow file alone does not prove a
successful run.
