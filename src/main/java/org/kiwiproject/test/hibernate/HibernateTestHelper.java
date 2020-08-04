package org.kiwiproject.test.hibernate;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Test utility for testing Hibernate-based code. This really makes sense when your tests are using a framework
 * that sets up a transactions before each test, executes the tests inside that transaction, and then rolls the
 * transactions back after each test. The methods here are useful to flush and clear the Hibernate {@link Session}
 * during test execution, otherwise Hibernate won't always automatically flush. In addition, you want to generally
 * clear the {@link Session} before performing certain operations to ensure the cache is cleared out.
 */
public final class HibernateTestHelper {

    @Getter
    private final SessionFactory sessionFactory;

    /**
     * Create a new helper with the specfied session factory. Operations involving the session delegate to this
     * session factory.
     *
     * @param sessionFactory the {@link SessionFactory} this helper should use
     */
    public HibernateTestHelper(SessionFactory sessionFactory) {
        this.sessionFactory = requireNotNull(sessionFactory);
    }

    /**
     * Return the "current" session.
     *
     * @return the current session as defined by the {@link SessionFactory#getCurrentSession()} method.
     */
    public Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * Flush the current session, effectively forcing Hibernate to perform database operations. Since this helper
     * assumes tests are executed in a transaction, the database operations are performed <em>but not committed</em>.
     *
     * @see SessionFactory#getCurrentSession()
     * @see Session#flush()
     */
    public void flushSession() {
        getCurrentSession().flush();
    }

    /**
     * Clears the current session.
     *
     * @see SessionFactory#getCurrentSession()
     * @see Session#clear()
     */
    public void clearSession() {
        getCurrentSession().clear();
    }

    /**
     * Flushes, then clears the current session.
     *
     * @see SessionFactory#getCurrentSession()
     * @see Session#flush()
     * @see Session#clear()
     */
    public void flushAndClearSession() {
        flushSession();
        clearSession();
    }
}
