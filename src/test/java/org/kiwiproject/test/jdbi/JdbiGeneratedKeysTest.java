package org.kiwiproject.test.jdbi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;

import java.util.stream.IntStream;

@DisplayName("JdbiGeneratedKeys")
class JdbiGeneratedKeysTest {

    @Test
    void shouldGetGeneratedId() {
        try (var handle = openDBI()) {
            createUsersTable(handle);
            IntStream.range(1, 7).forEach(i -> assertGetsGeneratedId(handle, i));
        }
    }

    private static void assertGetsGeneratedId(Handle handle, int i) {
        var update = createUserInsert(handle, i);
        var generatedKeys = update.executeAndReturnGeneratedKeys();
        long id = JdbiGeneratedKeys.generatedId(generatedKeys);

        assertThat(id).isEqualTo(i);
    }

    @Test
    void shouldGetGeneratedKey() {
        try (var handle = openDBI()) {
            createUsersTable(handle);
            IntStream.range(1, 8).forEach(i -> assertGetsGeneratedKey(handle, i));
        }
    }

    private static void assertGetsGeneratedKey(Handle handle, int i) {
        var update = createUserInsert(handle, i);
        var generatedKeys = update.executeAndReturnGeneratedKeys();
        long id = JdbiGeneratedKeys.generatedKey(generatedKeys, "id");

        assertThat(id).isEqualTo(i);
    }

    private static Update createUserInsert(Handle handle, int i) {
        return handle.createStatement("INSERT INTO users (name) VALUES (:name)")
                .bind("name", "Adam" + i);
    }

    private static Handle openDBI() {
        return DBI.open("jdbc:h2:mem:test");
    }

    private static void createUsersTable(org.skife.jdbi.v2.Handle handle) {
        handle.execute("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR)");
    }
}
