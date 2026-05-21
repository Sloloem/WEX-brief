Working list of useful commands and service setup information.

If you have Make installed, a makefile is provided with targets: `compose`, or `quick`.  These both build the service and deploy to the local docker environment, though `quick` does not run any tests.  Internal targets include `local`, `test`, and `docker` which build the image, run the tests, or bring up the docker environment, respectively.

Otherwise:
1. To build the Docker image: `./gradlew check bootBuildImage` (Omit `check` to skip tests)
2. To deploy the Composed environment with the new image: `docker-compose up --force-recreate --detach TransactionDB`

It's fine if you don't have Gradle, this project uses the gradle-wrapper which will automatically install a local copy of the correct version.

Assumptions:

This services lives in a nominally microservices architecture and so we need to be prepared for horizontal scaling and should make decisions with an eye towards cluster-safety.

The brief was slightly confusing around the date range requirment for finding an effective exchange rate, but the prep document seemed to clarify it.  Based on this the criteria seems to be: Use the rate from the date as close to the transaction date as possible, up to 6 months prior to the transaction date.  Otherwise throw an error.

A partitioning or archiving strategy will be implemented at some point to prevent the database tables from growing indefinitely.  This is primarily a concern with the transactions themselves since any transactions within the same basic time period will be serviceable from the same handful of exchange rate records.

Exchange Rate API retractions or corrections are rare enough to be handled with manual data cleanup, though since the pre-fetch always updates existing records new transactions will automatically fix any stale data.  Though we can re-order the priority of pre-fetched data vs the live-fetched fallback during conversion and remove the issue by favoring live API call and falling back to local data on API failures.

Quartz job and RestClient retry strategies can be tuned based on observations of Exchange Rate API stability and responsiveness.  Additionally this observation would also impact the decision to re-prioritize pre-fetch vs live-fetch suggested as a remedy to potential data fresh-ness issues.

Decisions:

I don't want to couple the performance or stability of this API to another API, so the system should pre-fetch exchange rates for transactions.
We can start the pre-fetch at transaction storage time but if we're assuming the API can go down at any point we should expect that it might be down at storage time.  Therefor the pre-fetch needs to be queued and run in the background so as not to impact storage functionality.

Since the pre-fetch wasn't explicitly called out in the brief, a synchronous fallback has been provided behind a configuration value which can act similar to a feature flag.

The requirements only state that an error be thrown if no suitable exchange rate could be found, a generic 404 is currently used since one is also thrown if the transaction ID can't be found.  I'd likely consider it up to the FE/UX team whether they need to differentiate between a bad transaction ID, bad currency name, and no data in range.

Rounding to the nearest cent was requested but no rounding preference (up/down/bankers) was described, half-up rounding was chosen as that's the most common understanding of rounding.  Though if this is a financial system, Bankers' rounding may be preferable.

Automated testing has been skipped for the API client since there is no specification to test against, integration testing would require invoking a live API and it's too risky to pin test stability to someone else's APIs.  If a schema or specification was provided, the client could be generated from it and a test could be written for a wrapper based on proper usage of the client AND/OR contract testing could be added via Spring Cloud's Contract testing utilities.

While trace and metric observability have been hooked up via Microsoft's Aspire OTLP service, logs are largely unconfigured.  OTLP is a standard and exact log formats and even output plugins are suggested by organizational log aggregation setups like ELK or external services like Plunk or Coralogix so I didn't want to take a stance on it and leave it open to hypotheticals.

No GitHub Actions workflows have been defined, largely because there was no information about CI/CD environment or deployment mechanisms in the brief.  Again, this has been left open to hypotheticals.