package org.kiwiproject.test.liquibase;

import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotBlank;
import static org.kiwiproject.base.KiwiPreconditions.checkArgumentNotNull;
import static org.kiwiproject.base.KiwiStrings.f;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import liquibase.command.CommandResults;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import lombok.experimental.UtilityClass;
import org.kiwiproject.jdbc.UncheckedSQLException;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * A utility class for performing Liquibase operations.
 */
@UtilityClass
public class LiquibaseTestHelpers {

    /**
     * Runs Liquibase database migrations.
     *
     * @param dataSource    the {@link DataSource} to use
     * @param changelogFile the location of the changelog file, e.g., {@code migrations.xml}
     * @return the migration results provided by Liquibase
     */
    @CanIgnoreReturnValue
    public static CommandResults runMigrations(DataSource dataSource, String changelogFile) {
        checkArgumentNotNull(dataSource);
        checkArgumentNotBlank(changelogFile);

        try (var conn = dataSource.getConnection()) {
            var liquibaseDb = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));

            return runMigrations(liquibaseDb, changelogFile);
        } catch (LiquibaseException liquibaseException) {
            throw new UncheckedLiquibaseException("Error finding database implementation", liquibaseException);
        } catch (SQLException sqlException) {
            throw new UncheckedSQLException("Error getting Connection from DataSource", sqlException);
        }
    }

    /**
     * Runs Liquibase database migrations.
     *
     * @param database      the Liquibase {@link Database} to use
     * @param changelogFile the location of the changelog file, e.g., {@code my-test/migrations.xml}
     * @return the migration results provided by Liquibase
     */
    @CanIgnoreReturnValue
    public static CommandResults runMigrations(Database database, String changelogFile) {
        checkArgumentNotNull(database);
        checkArgumentNotBlank(changelogFile);

        try {
            var updateCommand = new CommandScope(UpdateCommandStep.COMMAND_NAME)
                    .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                    .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changelogFile);

            return updateCommand.execute();
        } catch (LiquibaseException e) {
            var message = f("Error running Liquibase update for {} database using migrations file {}",
                    database.getDisplayName(), changelogFile);
            throw new UncheckedLiquibaseException(message, e);
        }
    }
}
