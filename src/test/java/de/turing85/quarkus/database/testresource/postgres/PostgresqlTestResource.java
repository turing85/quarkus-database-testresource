package de.turing85.quarkus.database.testresource.postgres;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Getter
public class PostgresqlTestResource
    implements QuarkusTestResourceConfigurableLifecycleManager<WithPostgresDatabaseContainers>,
    DevServicesContext.ContextAware {
  private static final String LABEL_QUARKUS_DATASOURCE_NAME = "quarkus.datasource.name";
  private static final RandomStringUtils SECURE = RandomStringUtils.secure();
  // @formatter:off
  public static final DockerImageName IMAGE = DockerImageName
      .parse("postgres")
      .withTag("15.10-alpine3.21")
      .withRegistry("docker.io")
      .asCompatibleSubstituteFor("postgres");
  // @formatter:on

  private final Set<String> names = new HashSet<>();
  private final List<JdbcDatabaseContainer<?>> containers = new ArrayList<>();

  private String containerNetworkId = null;

  @Override
  public void init(final WithPostgresDatabaseContainers annotation) {
    names().addAll(Arrays.asList(annotation.value()));
  }

  @Override
  public void setIntegrationTestContext(final DevServicesContext context) {
    containerNetworkId = context.containerNetworkId().orElse(null);
  }

  @Override
  public Map<String, String> start() {
    if (!containers.isEmpty()) {
      stop();
    }
    // @formatter:off
    return names().stream()
        .map(this::createPostgresContainer)
        .map(this::startContainerAndExtractConfig)
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    // @formatter:on
  }

  private JdbcDatabaseContainer<?> createPostgresContainer(final String name) {
    // @formatter:off
    return new PostgreSQLContainer<>(IMAGE)
        .withLabel(LABEL_QUARKUS_DATASOURCE_NAME, name)
        .withDatabaseName("db-%s".formatted(SECURE.next(10, true, true)))
        .withUsername("username-%s".formatted(SECURE.next(10, true, true)))
        .withPassword("password-%s".formatted(SECURE.next(10, true, true)))
        .withCreateContainerCmdModifier(cmd -> cmd.withName(randomize(name)))
        .withNetworkMode(containerNetworkId);
    // @formatter:on
  }

  private static String randomize(final String name) {
    return "%s-%s".formatted(sanitizeName(name), UUID.randomUUID());
  }

  private static String sanitizeName(final String name) {
    return name.replaceAll("[^0-9a-zA-Z_-]", "_").replaceAll("^_", "a_");
  }

  private Map<String, String> startContainerAndExtractConfig(
      final JdbcDatabaseContainer<?> container) {
    container.start();
    containers.add(container);
    return extractConfigs(container);
  }

  private static Map<String, String> extractConfigs(final JdbcDatabaseContainer<?> container) {
    return Map.of(constructJdbcUrlPropertyName(container), container.getJdbcUrl(),
        constructUsernamePropertyName(container), container.getUsername(),
        constructPasswordPropertyName(container), container.getPassword());
  }

  private static String constructJdbcUrlPropertyName(final JdbcDatabaseContainer<?> container) {
    return "%s.jdbc.url".formatted(constructDbNameProperty(container));
  }

  private static String constructUsernamePropertyName(final JdbcDatabaseContainer<?> container) {
    return "%s.username".formatted(constructDbNameProperty(container));
  }

  private static String constructPasswordPropertyName(final JdbcDatabaseContainer<?> container) {
    return "%s.password".formatted(constructDbNameProperty(container));
  }

  private static String constructDbNameProperty(final JdbcDatabaseContainer<?> container) {
    return "quarkus.datasource%s".formatted(constructDatasourceNameProperty(container));
  }

  private static String constructDatasourceNameProperty(final JdbcDatabaseContainer<?> container) {
    final String datasourceName = container.getLabels().get(LABEL_QUARKUS_DATASOURCE_NAME);
    if (Objects.equals("<default>", datasourceName)) {
      return "";
    }
    return ".%s".formatted(datasourceName);
  }

  @Override
  public void stop() {
    containers.forEach(JdbcDatabaseContainer::close);
    containers.clear();
  }
}
