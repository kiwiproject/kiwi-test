package org.kiwiproject.test.jakarta.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

/**
 * Yes, this is really just a "restating the obvious test" to ensure no one "accidentally" changes something
 * without thinking about what they're doing. And yes, it uses mock objects instead of real JPA ones, since
 * we only need to verify the expected method calls and assume JPA knows what it's doing.
 */
@DisplayName("JpaTestHelper")
public class JpaTestHelperTest {

    private JpaTestHelper helper;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        helper = new JpaTestHelper(entityManager);
    }

    @Test
    void shouldJoinTransaction() {
        var joinedHelper = helper.joinTransaction();

        assertThat(joinedHelper).isSameAs(helper);

        verify(entityManager, only()).joinTransaction();
    }

    @Test
    void shouldFlushEntityManager() {
        helper.flushEntityManager();

        verify(entityManager, only()).flush();
    }

    @Test
    void shouldClearEntityManager() {
        helper.clearEntityManager();

        verify(entityManager, only()).clear();
    }

    @Test
    void shouldFlushAndClearEntityManager() {
        helper.flushAndClearEntityManager();

        var inOrder = inOrder(entityManager);
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        verifyNoMoreInteractions(entityManager);
    }
}
