# AI Tool Usage

The assignment encourages using AI assistants and asks candidates to explain
what they were used for, what issues surfaced, and how those were corrected.
This document records that.

## What AI was used for

- **Project scaffolding** — generating the Gradle build file, package layout, and
  Spring Boot boilerplate (entities, repositories, DTOs, controllers).
- **JWT + Spring Security wiring** — drafting the `JwtService`, authentication
  filter, and `SecurityConfig`, which is repetitive, error-prone boilerplate.
- **Test generation** — first-pass unit and integration tests, then refined by hand.
- **README / API documentation** — structuring the docs.

## Issues found and how they were corrected

1. **Gradle version too old for Spring Boot 3.3.**
   The reused wrapper pinned Gradle 7.5, which cannot build Spring Boot 3.3.
   *Fix:* bumped the wrapper `distributionUrl` to Gradle 8.10.2.

2. **Deprecated jjwt API.**
   Initial JWT code used the pre-0.12 builder API (`setClaims`, `signWith(key, alg)`).
   *Fix:* migrated to the 0.12.x fluent API (`claims()`, `signWith(key)`,
   `parser().verifyWith(...)`).

3. **Integration tests failed with a foreign-key constraint violation.**
   `@BeforeEach` deleted products/users while orders from a previous test method
   still referenced them.
   *Fix:* delete in FK-safe order — `orders` first, then `users`/`products`.

4. **Unauthenticated requests returned 403 instead of 401.**
   Spring Security's default for anonymous access to a protected resource is 403.
   *Fix:* added an `HttpStatusEntryPoint(UNAUTHORIZED)` so missing/invalid tokens
   correctly return 401, with 403 reserved for authenticated-but-unauthorised.

5. **Business rules the AI initially omitted.**
   The first draft allowed arbitrary status updates and didn't handle stock.
   *Fix:* added an explicit `OrderStatus` state machine (`canTransitionTo`),
   enforced "cancel only when PENDING", and added stock reservation/restoration.

## Verification

All changes were validated by running `./gradlew test` (16 tests passing) rather
than trusting generated code as-is. AI output was treated as a draft and reviewed
for correctness, security, and adherence to the assignment's business rules.
