package org.kiwiproject.test.h2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@DisplayName("H2DatabaseTestHelper")
class H2DatabaseTestHelperTest {

    @Test
    void shouldCreateFileBasedDatabase() {
        var db = H2DatabaseTestHelper.buildH2FileBasedDatabase();

        assertThat(db.getDirectory()).exists();
        assertThat(db.getDataSource()).isNotNull();
        assertThat(db.getUrl()).isEqualTo("jdbc:h2:%s/testdb", db.getDirectory().getAbsolutePath());
        assertThatCode(() -> {
            var connection = db.getDataSource().getConnection();
            var metaData = connection.getMetaData();
            assertThat(metaData).isNotNull();
            connection.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldCreateDatabaseInJavaTmpDir() {
        var db = H2DatabaseTestHelper.buildH2FileBasedDatabase();

        var javaTmpDir = SystemUtils.getJavaIoTmpDir();
        assertThat(db.getDirectory()).hasParent(javaTmpDir);
    }

    @Test
    void shouldThrow_WhenDirectoryNotWriteable(@TempDir Path tempDir) {
        var tempDirPath = tempDir.toString();
        var notWritable = new File(tempDirPath, "notWritable");
        assertThat(notWritable.mkdir()).isTrue();
        assertThat(notWritable.setWritable(false)).isTrue();
        var subDirOfNotWritable = new File(notWritable, "subDir");

        assertThatIllegalStateException()
                .isThrownBy(() -> H2DatabaseTestHelper.buildH2DataSource(subDirOfNotWritable))
                .withMessageStartingWith("Could not create directory for H2 test database: ")
                .withMessageEndingWith(subDirOfNotWritable.toString())
                .withCauseInstanceOf(IOException.class);
    }
}
