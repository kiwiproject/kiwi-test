package org.kiwiproject.test.junit.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@DisplayName("Jdbi3MultiDaoExtension")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
class Jdbi3MultiDaoExtensionTest {

    @RegisterExtension
    static final H2FileBasedDatabaseExtension DATABASE_EXTENSION = new H2FileBasedDatabaseExtension();

    @RegisterExtension final Jdbi3MultiDaoExtension multiDaoExtension = Jdbi3MultiDaoExtension.builder()
            .dataSource(DATABASE_EXTENSION.getDataSource())
            .slf4jLoggerName(Jdbi3MultiDaoExtensionTest.class.getName())
            .plugin(new H2DatabasePlugin())
            .daoType(PersonDao.class)
            .daoType(PlaceDao.class)
            .daoType(ThingDao.class)
            .build();

    private Handle handle;
    private PersonDao personDao;
    private PlaceDao placeDao;
    private ThingDao thingDao;

    @BeforeAll
    static void beforeAll() throws SQLException {
        try (var conn = DATABASE_EXTENSION.getDataSource().getConnection();
             var ps1 = conn.prepareStatement("create table test_people(name varchar, age integer)");
             var ps2 = conn.prepareStatement("create table test_places(name varchar, location varchar)");
             var ps3 = conn.prepareStatement("create table test_things(name varchar, description varchar)")) {
            ps1.execute();
            ps2.execute();
            ps3.execute();
        }
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        LOG.trace("Executing test: {}", testInfo.getDisplayName());
        handle = multiDaoExtension.getHandle();
        personDao = multiDaoExtension.getDao(PersonDao.class);
        placeDao = multiDaoExtension.getDao(PlaceDao.class);
        thingDao = multiDaoExtension.getDao(ThingDao.class);
    }

    @Test
    @Order(0)
    void shouldProvideMetadata() {
        assertThat(multiDaoExtension.getJdbi()).isNotNull();
        assertThat(multiDaoExtension.getDaoTypes())
                .containsExactlyInAnyOrder(PersonDao.class, PlaceDao.class, ThingDao.class);
        assertThat(multiDaoExtension.getDaos())
                .hasSize(3)
                .containsEntry(PersonDao.class, personDao)
                .containsEntry(PlaceDao.class, placeDao)
                .containsEntry(ThingDao.class, thingDao);
    }

    @Test
    @Order(1)
    void shouldBeginTransactionResultingInDisabledAutoCommit() throws SQLException {
        assertThat(handle.getConnection().getAutoCommit()).isFalse();
    }

    @Test
    @Order(2)
    void shouldNotSeeAnyDataBeforeTestDataInserted() {
        assertNoPeopleOrPlacesOrThingsExist();
    }

    @Test
    @Order(3)
    void shouldInsertUsingHandle_AndThenFindTestData() {
        handle.execute("insert into test_people values ('Bob', 42)");
        handle.execute("insert into test_people values ('Alice', 36)");

        handle.execute("insert into test_places values ('Snowmass', 'Colorado')");
        handle.execute("insert into test_places values ('Big Sky', 'Montana')");
        handle.execute("insert into test_places values ('Solitude', 'Utah')");

        handle.execute("insert into test_things values ('Pen', 'Something to write with')");
        handle.execute("insert into test_things values ('Laptop', 'A portable computer')");

        assertThat(personDao.findAll()).hasSize(2);
        assertThat(placeDao.findAll()).hasSize(3);
        assertThat(thingDao.findAll()).hasSize(2);
    }

    @Test
    @Order(4)
    void shouldNotSeeAnyDataFromFirstInsertTest() {
        assertNoPeopleOrPlacesOrThingsExist();
    }

    @Test
    @Order(5)
    void shouldInsertUsingDaos_AndThenFindTestData() {
        personDao.insert(new Person("Bob", 42));
        placeDao.insert(new Place("Snowmass", "Colorado"));
        thingDao.insert(new Thing("Pen", "Something to write with"));

        assertThat(personDao.findAll()).hasSize(1);
        assertThat(placeDao.findAll()).hasSize(1);
        assertThat(thingDao.findAll()).hasSize(1);
    }

    @Test
    @Order(6)
    void shouldNotSeeAnyDataFrom_DaoInsertTest() {
        assertNoPeopleOrPlacesOrThingsExist();
    }

    @Test
    @Order(7)
    void shouldStillHaveAutoCommitDisabled() throws SQLException {
        assertThat(handle.getConnection().getAutoCommit()).isFalse();
    }

    private void assertNoPeopleOrPlacesOrThingsExist() {
        assertThat(personDao.findAll()).isEmpty();
        assertThat(placeDao.findAll()).isEmpty();
        assertThat(thingDao.findAll()).isEmpty();
    }

    // Model classes (must be public for JDBI to bind)

    interface Named {
        String getName();
    }

    @SuppressWarnings("WeakerAccess")
    @Value
    public static class Person implements Named {
        String name;
        int age;
    }

    @SuppressWarnings("WeakerAccess")
    @Value
    public static class Place implements Named {
        String name;
        String location;
    }

    @SuppressWarnings("WeakerAccess")
    @Value
    public static class Thing implements Named {
        String name;
        String description;
    }

    // Mapper classes (must be public for JDBI to instantiate)
    public static class PersonMapper implements RowMapper<Person> {
        @Override
        public Person map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Person(rs.getString("name"), rs.getInt("age"));
        }
    }

    public static class PlaceMapper implements RowMapper<Place> {
        @Override
        public Place map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Place(rs.getString("name"), rs.getString("location"));
        }
    }

    public static class ThingMapper implements RowMapper<Thing> {
        @Override
        public Thing map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Thing(rs.getString("name"), rs.getString("description"));
        }
    }

    // DAOs

    @RegisterRowMapper(PersonMapper.class)
    private interface PersonDao {
        @SqlQuery("select * from test_people order by name")
        List<Person> findAll();

        @SqlUpdate("insert into test_people values (:name, :age)")
        void insert(@BindBean Person person);
    }

    @RegisterRowMapper(PlaceMapper.class)
    private interface PlaceDao {
        @SqlQuery("select * from test_places order by name")
        List<Place> findAll();

        @SqlUpdate("insert into test_places values (:name, :location)")
        void insert(@BindBean Place place);
    }

    @RegisterRowMapper(ThingMapper.class)
    private interface ThingDao {
        @SqlQuery("select * from test_things order by name")
        List<Thing> findAll();

        @SqlUpdate("insert into test_things values (:name, :description)")
        void insert(@BindBean Thing thing);
    }
}
