package org.kiwiproject.test.dropwizard.jdbi2;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.kiwiproject.logging.LazyLogParameterSupplier.lazy;

import io.dropwizard.jdbi.GuavaOptionalContainerFactory;
import io.dropwizard.jdbi.ImmutableListContainerFactory;
import io.dropwizard.jdbi.ImmutableSetContainerFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.jdbi.args.GuavaOptionalArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalInstantArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalJodaTimeArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalLocalDateArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalLocalDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalOffsetTimeArgumentFactory;
import io.dropwizard.jdbi.args.GuavaOptionalZonedTimeArgumentFactory;
import io.dropwizard.jdbi.args.InstantArgumentFactory;
import io.dropwizard.jdbi.args.InstantMapper;
import io.dropwizard.jdbi.args.JodaDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.JodaDateTimeMapper;
import io.dropwizard.jdbi.args.LocalDateArgumentFactory;
import io.dropwizard.jdbi.args.LocalDateMapper;
import io.dropwizard.jdbi.args.LocalDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.LocalDateTimeMapper;
import io.dropwizard.jdbi.args.OffsetDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.OffsetDateTimeMapper;
import io.dropwizard.jdbi.args.OptionalArgumentFactory;
import io.dropwizard.jdbi.args.OptionalDoubleArgumentFactory;
import io.dropwizard.jdbi.args.OptionalDoubleMapper;
import io.dropwizard.jdbi.args.OptionalInstantArgumentFactory;
import io.dropwizard.jdbi.args.OptionalIntArgumentFactory;
import io.dropwizard.jdbi.args.OptionalIntMapper;
import io.dropwizard.jdbi.args.OptionalJodaTimeArgumentFactory;
import io.dropwizard.jdbi.args.OptionalLocalDateArgumentFactory;
import io.dropwizard.jdbi.args.OptionalLocalDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.OptionalLongArgumentFactory;
import io.dropwizard.jdbi.args.OptionalLongMapper;
import io.dropwizard.jdbi.args.OptionalOffsetDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.OptionalZonedDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.ZonedDateTimeArgumentFactory;
import io.dropwizard.jdbi.args.ZonedDateTimeMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.jar.KiwiJars;
import org.skife.jdbi.v2.DBI;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Dropwizard JDBI utilities for dropwizard-jdbi which is JDBI 2 not 3.
 * <p>
 * <strong>NOTE:</strong> Please make sure to read the documentation for
 * {@link #registerDefaultDropwizardJdbiFeatures(DBI, String, TimeZone)} whenever the Dropwizard version is updated.
 */
@UtilityClass
@Slf4j
public class DropwizardJdbi {

    /**
     * Expected version of Dropwizard.
     * <p>
     * <strong>NOTE:</strong> Please make sure to read the documentation for
     * {@link #registerDefaultDropwizardJdbiFeatures(DBI, String, TimeZone)} whenever the Dropwizard version is updated.
     */
    public static final String EXPECTED_DROPWIZARD_VERSION = "2.0.12";

    /**
     * Detected version of Dropwizard using the {@link GuavaOptionalArgumentFactory} from the
     * {@code dropwizard-jdbi2} JAR file.
     */
    public static final String DETECTED_DROPWIZARD_VERSION = detectDropwizardVersion();

    /**
     * Detect the Dropwizard version, using {@link GuavaOptionalArgumentFactory} as a "representative" class that
     * resides in the {@code dropwizard-jdbi} JAR file.
     *
     * @return the detected version of Dropwizard
     */
    public static String detectDropwizardVersion() {
        var directoryPath = KiwiJars.getDirectoryPath(GuavaOptionalArgumentFactory.class).orElse("");
        if (isBlank(directoryPath)) {
            return "<Unknown>";
        }

        var lastSlashIndex = directoryPath.lastIndexOf('/');
        return directoryPath.substring(lastSlashIndex + 1);
    }

    /**
     * Configures the given {@link DBI} instance with all the goodies that Dropwizard adds, e.g. various argument
     * factories, column mappers, etc. This might be useful in database tests in a Dropwizard application and you want
     * to make sure the {@link DBI} instance as it would be configured in the application.
     * <p>
     * This is necessary since, unfortunately, the Dropwizard {@link io.dropwizard.jdbi.DBIFactory} does not publicly
     * expose the configuration method. Therefore, this method is copied (with minor modifications) directly from
     * the <em>protected</em> {@code DBIFactory#configure(DBI, PooledDataSourceFactory)} method. The changes are
     * to accommodate supplying the database driver class and timezone as arguments, whereas in the original configure
     * method they are obtained in the method.
     * <p>
     * In addition, this method logs a warning if the detected and expected Dropwizard versions do not match.
     *
     * @param dbi              the DBI instance to use
     * @param driverClazz      the database driver class
     * @param nullableTimeZone a {@link TimeZone} or null
     * @implNote The time zone is needed for cases when the database operates in a different time zone then the
     * application and it doesn't use the SQL type 'TIMESTAMP WITH TIME ZONE'. In such cases information about the
     * time zone should be  explicitly passed to the JDBC driver. See
     * {@code io.dropwizard.jdbi.DBIFactory#databaseTimeZone()} which has protected access in DBIFactory.
     */
    public static void registerDefaultDropwizardJdbiFeatures(DBI dbi,
                                                             String driverClazz,
                                                             @Nullable TimeZone nullableTimeZone) {

        logWarningIfDropwizardVersionMismatch();

        var timeZone = Optional.ofNullable(nullableTimeZone);
        LOG.trace("Register default Dropwizard {} JDBI features (driver class: {}, time zone: {}",
                EXPECTED_DROPWIZARD_VERSION, driverClazz, lazy(() -> timeZone.map(TimeZone::getID).orElse(null)));

        dbi.registerArgumentFactory(new GuavaOptionalArgumentFactory(driverClazz));
        dbi.registerArgumentFactory(new OptionalArgumentFactory(driverClazz));
        dbi.registerArgumentFactory(new OptionalDoubleArgumentFactory());
        dbi.registerArgumentFactory(new OptionalIntArgumentFactory());
        dbi.registerArgumentFactory(new OptionalLongArgumentFactory());
        dbi.registerColumnMapper(new OptionalDoubleMapper());
        dbi.registerColumnMapper(new OptionalIntMapper());
        dbi.registerColumnMapper(new OptionalLongMapper());
        dbi.registerContainerFactory(new ImmutableListContainerFactory());
        dbi.registerContainerFactory(new ImmutableSetContainerFactory());
        dbi.registerContainerFactory(new GuavaOptionalContainerFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());

        dbi.registerArgumentFactory(new JodaDateTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new LocalDateArgumentFactory());
        dbi.registerArgumentFactory(new LocalDateTimeArgumentFactory());
        dbi.registerArgumentFactory(new InstantArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new OffsetDateTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new ZonedDateTimeArgumentFactory(timeZone));

        // Should be registered after GuavaOptionalArgumentFactory to be processed first
        dbi.registerArgumentFactory(new GuavaOptionalJodaTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new GuavaOptionalLocalDateArgumentFactory());
        dbi.registerArgumentFactory(new GuavaOptionalLocalDateTimeArgumentFactory());
        dbi.registerArgumentFactory(new GuavaOptionalInstantArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new GuavaOptionalOffsetTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new GuavaOptionalZonedTimeArgumentFactory(timeZone));

        // Should be registered after OptionalArgumentFactory to be processed first
        dbi.registerArgumentFactory(new OptionalJodaTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new OptionalLocalDateArgumentFactory());
        dbi.registerArgumentFactory(new OptionalLocalDateTimeArgumentFactory());
        dbi.registerArgumentFactory(new OptionalInstantArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new OptionalOffsetDateTimeArgumentFactory(timeZone));
        dbi.registerArgumentFactory(new OptionalZonedDateTimeArgumentFactory(timeZone));

        dbi.registerColumnMapper(new JodaDateTimeMapper(timeZone));
        dbi.registerColumnMapper(new LocalDateMapper());
        dbi.registerColumnMapper(new LocalDateTimeMapper());
        dbi.registerColumnMapper(new InstantMapper(timeZone));
        dbi.registerColumnMapper(new OffsetDateTimeMapper(timeZone));
        dbi.registerColumnMapper(new ZonedDateTimeMapper(timeZone));
    }

    private static void logWarningIfDropwizardVersionMismatch() {
        if (!DETECTED_DROPWIZARD_VERSION.equals(EXPECTED_DROPWIZARD_VERSION)) {
            LOG.warn("Expected Dropwizard version {} but was {}." +
                            " This might cause issues in tests if version {} changed the default registered JDBI features!",
                    EXPECTED_DROPWIZARD_VERSION, DETECTED_DROPWIZARD_VERSION, DETECTED_DROPWIZARD_VERSION);
        }
    }
}
