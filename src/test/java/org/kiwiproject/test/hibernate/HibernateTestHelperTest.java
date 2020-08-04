package org.kiwiproject.test.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Yes, this is really just a "restating the obvious test" to ensure no one "accidentally" changes something
 * without thinking about what they're doing. And yes, it uses mock objects instead of real Hibernate ones, since
 * we only need to verify the expected method calls and assume Hibernate knows what it's doing.
 */
@DisplayName("HibernateTestHelper")
class HibernateTestHelperTest {

    private HibernateTestHelper helper;
    private SessionFactory sessionFactory;
    private Session session;

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        sessionFactory = mock(SessionFactory.class);
        helper = new HibernateTestHelper(sessionFactory);
    }

    @Test
    void shouldGetSessionFactory() {
        assertThat(helper.getSessionFactory()).isSameAs(sessionFactory);
    }

    @Test
    void shouldGetCurrentSession() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        var currentSession = helper.getCurrentSession();
        assertThat(currentSession).isSameAs(session);

        verifyGetCurrentSession();
        verifyNoInteractions(session);
    }

    private void verifyGetCurrentSession() {
        verify(sessionFactory).getCurrentSession();
        verifyNoMoreInteractions(sessionFactory);
    }

    @Test
    void shouldFlushSession() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        helper.flushSession();

        verifyGetCurrentSession();
        verify(session).flush();
        verifyNoMoreInteractions(session);
    }

    @Test
    void shouldClearSession() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        helper.clearSession();

        verifyGetCurrentSession();
        verify(session).clear();
        verifyNoMoreInteractions(session);
    }

    @Test
    void shouldFlushAndClearHibernateSession() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        helper.flushAndClearSession();

        var inOrder = inOrder(sessionFactory, session);
        inOrder.verify(sessionFactory).getCurrentSession();
        inOrder.verify(session).flush();
        inOrder.verify(sessionFactory).getCurrentSession();
        inOrder.verify(session).clear();
        inOrder.verifyNoMoreInteractions();
    }
}
