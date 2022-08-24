package org.kiwiproject.test.junit.jupiter;

import static java.util.Objects.nonNull;
import static org.kiwiproject.test.junit.jupiter.JupiterHelpers.isTestClassNested;
import static org.kiwiproject.test.junit.jupiter.JupiterHelpers.testClassNameOrNull;

import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.kiwiproject.test.h2.H2DatabaseTestHelper;
import org.kiwiproject.test.h2.H2FileBasedDatabase;

/**
 * JUnit Jupiter extension that creates a file-based H2 database before all tests and deletes it after all tests
 * have executed. It also provides for injection of the database into test lifecycle methods that declare
 * a {@link H2FileBasedDatabase} annotated with {@link H2Database}.
 * <p>
 * You can register the extension via {@code ExtendWith} and then use parameter resolution with the {@link H2Database}
 * annotation to obtain the {@link H2FileBasedDatabase} instance in a lifecycle or test method. Example:
 * <pre>
 * {@literal @}ExtendWith(H2FileBasedDatabaseExtension.class)
 *  class MyFirstTest {
 *
 *     {@literal @}BeforeEach
 *      void setUp(@H2Database H2FileBasedDatabase database) { ... }
 *
 *     {@literal @}Test
 *      void shouldDoSomethingWithTheDatabase(@H2Database H2FileBasedDatabase database) { ... }
 *  }
 * </pre>
 * <p>
 * Alternatively, if you need to supply another extension with the H2 database, then you can register the extension
 * on a static field using {@code @RegisterExtension}. Here is an example using a theoretical extension named
 * {@code DaoExtension} that requires a {@code DataSource}. The data source is obtained from the
 * {@link H2FileBasedDatabase} provided by this extension:
 * <pre>
 * class MySecondTest {
 *
 *    {@literal @}RegisterExtension
 *     static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();
 *
 *    {@literal @}RegisterExtension
 *     final DaoExtension&lt;PersonDao&gt; jdbi3DaoExtension =
 *             DaoExtension.&lt;PersonDao&gt;builder()
 *                     .daoType(PersonDao.class)
 *                     .dataSource(DATABASE_EXTENSION.getDataSource())  // supply the DataSource here
 *                     .plugin(new H2DatabasePlugin())
 *                     .build();
 * }
 * </pre>
 * <strong>NOTE:</strong>The second example only works when the extension that requires the {@link H2FileBasedDatabase}
 * is an <em>instance</em> field. If it is static, it will not work!
 * <p>
 * When using this extension via {@code @RegisterExtension}, you can use the getter methods to retrieve the entire
 * {@link H2FileBasedDatabase} object or its individual properties. When using it with {@code @ExtendWith} and an
 * injected parameter, you obviously have direct access to the {@link H2FileBasedDatabase} object.
 * <p>
 * This requires <a href="https://mvnrepository.com/artifact/com.h2database/h2">h2</a> and
 * <a href="https://mvnrepository.com/artifact/commons-io/commons-io">commons-io</a> to be
 * available at runtime when tests are executing.
 */
@Slf4j
public class H2FileBasedDatabaseExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final String DATABASE_KEY = "database";

    @Getter
    @Delegate
    private H2FileBasedDatabase database;

    /**
     * Creates a new H2 file-based database if a database does not exist and the test class is not
     * annotated with {@link Nested}.
     *
     * @param context extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        if (nonNull(database)) {
            LOG.trace("A database already exists (we are probably inside a @Nested test class) so not doing anything");
            return;
        }

        LOG.trace("Database does not exist; create it");
        database = H2DatabaseTestHelper.buildH2FileBasedDatabase();

        var namespace = createNamespace();
        context.getStore(namespace).put(DATABASE_KEY, database);
        LOG.trace("Created and stored database: {}", database);
    }

    /**
     * Deletes the H2 file-based database if the test class is not annotated with {@link Nested}.
     *
     * @param context extension context
     * @throws Exception if the database directory could not be deleted
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (isTestClassNested(context)) {
            LOG.trace("We're in nested class {}, so NOT deleting the database", testClassNameOrNull(context));
        } else {
            LOG.trace("Deleting database: {}", database);
            FileUtils.deleteDirectory(database.getDirectory());
        }
    }

    /**
     * Does the parameter need to be resolved?
     *
     * @param parameterContext parameter context
     * @param extensionContext extension context
     * @return true if the {@code parameterContext} is annotated with {@link H2Database}
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(H2Database.class);
    }

    /**
     * Resolve the parameter annotated with {@link H2Database} into a {@link H2FileBasedDatabase}.
     *
     * @param parameterContext parameter context
     * @param extensionContext extension context
     * @return the resolved {@link H2FileBasedDatabase}
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var namespace = createNamespace();

        return getDatabase(extensionContext, namespace);
    }

    private ExtensionContext.Namespace createNamespace() {
        return ExtensionContext.Namespace.create(getClass(), "H2FileBasedDatabase", Thread.currentThread().getName());
    }

    private H2FileBasedDatabase getDatabase(ExtensionContext context, ExtensionContext.Namespace namespace) {
        return context.getStore(namespace).get(DATABASE_KEY, H2FileBasedDatabase.class);
    }
}
