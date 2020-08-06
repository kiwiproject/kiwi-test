package org.kiwiproject.test.dropwizard.jdbi2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.h2.Driver;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skife.jdbi.v2.DBI;

import java.util.TimeZone;

class DropwizardJdbiTest {

    private static final String UPDATED_DROPWIZARD_MESSAGE =
            "If this fails, most likely it is because the version of dropwizard-jdbi2 was changed." +
                    " Fix this by changing the expected value to match the version in the POM," +
                    " updating DropwizardJdbi#EXPECTED_DROPWIZARD_VERSION, and by updating the" +
                    " DropwizardJdbi#registerDefaultDropwizardJdbiFeatures method so it matches what" +
                    " DBIFactory#configure does.";

    @Test
    void shouldFindExpectedDropwizardVersion() {
        assertThat(DropwizardJdbi.EXPECTED_DROPWIZARD_VERSION)
                .describedAs(UPDATED_DROPWIZARD_MESSAGE)
                .isEqualTo("2.0.12");
    }

    @Test
    void shouldDetectDropwizardVersion() {
        assertThat(DropwizardJdbi.DETECTED_DROPWIZARD_VERSION)
                .describedAs(UPDATED_DROPWIZARD_MESSAGE)
                .isEqualTo("2.0.12");
    }

    /**
     * These are "smoke tests" to simply make sure the registerDefaultDropwizardJdbiFeatures method doesn't throw
     * any exceptions when called. Since the code was copy/pasted from Dropwizard, we assume there isn't a huge need
     * to re-test the code. Thus these "tests" don't actually verify what JDBI features get registered, again since
     * we assume Dropwizard knows what it is doing and that we are able to copy/paste code without majorly screwing
     * it up.
     */
    @Nested
    class SmokeTests {

        @Test
        void shouldRegisterDefaultDropwizardJdbiFeatures_WithoutTimeZone() {
            var dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:testdb");
            var dbi = new DBI(dataSource);
            var driverClazz = Driver.class.getName();

            assertThatCode(() -> DropwizardJdbi.registerDefaultDropwizardJdbiFeatures(dbi, driverClazz, null))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldRegisterDefaultDropwizardJdbiFeatures_WithTimeZone() {
            var dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:testdb");
            var dbi = new DBI(dataSource);
            var driverClazz = Driver.class.getName();

            var sydneyTimeZone = TimeZone.getTimeZone("Australia/Sydney");

            assertThatCode(() -> DropwizardJdbi.registerDefaultDropwizardJdbiFeatures(dbi, driverClazz, sydneyTimeZone))
                    .doesNotThrowAnyException();
        }
    }

}
