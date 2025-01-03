package de.turing85.quarkus.database.testresource;

import de.turing85.quarkus.database.testresource.postgres.WithPostgresDatabaseContainers;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@WithPostgresDatabaseContainers({"<default>", "one", "two"})
class ReadyTest {
  @Test
  void isReady() {
    // @formatter:off
    RestAssured
        .when().get("/q/health")
        .then()
            .body("containsKey('checks')", is(true))
            .body("checks.any { it.containsKey('name') && it.name.startsWith('Database') }",
                is(true))
            .body("checks.find { it.name.startsWith('Database') }.data.containsKey('<default>')",
                is(true))
            .body("checks.find { it.name.startsWith('Database') }.data.'<default>'", is("UP"))
            .body("checks.find { it.name.startsWith('Database') }.data.containsKey('one')",
                is(true))
            .body("checks.find { it.name.startsWith('Database') }.data.one", is("UP"))
            .body("checks.find { it.name.startsWith('Database') }.data.containsKey('two')",
                is(true))
            .body("checks.find { it.name.startsWith('Database') }.data.two", is("UP"));
    // @formatter: on
  }
}
