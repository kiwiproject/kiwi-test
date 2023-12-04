package org.kiwiproject.test.jakarta.persistence;

import static org.kiwiproject.base.KiwiPreconditions.requireNotNull;

import com.google.common.annotations.Beta;

import lombok.Getter;

import jakarta.persistence.EntityManager;

/**
 * Test utility for testing JPA-based code. This is mainly useful when your tests are using a framework
 * that sets up a transaction before each test, executes the test inside that transaction, and then rolls
 * back the transaction after the test. The methods here are useful to flush and clear the {@link EntityManager}
 * during test execution, otherwise JPA (e.g., Hibernate) won't always automatically flush. In addition,
 * you want to generally clear the {@link EntityManager} before performing certain operations to ensure
 * the cache is cleared out,  for example after inserting test data but before performing a query to ensure
 * the test data is returned.
 * <p>
 * If needed yu can also join the existing transaction inside your test, to make sure the DAOs under
 * test are using the same EntityManager! For example, inside a setup method:
 * <pre>
 * {@literal @}BeforeEach
 *  void setUp(@Autowired EntityManagerFactory entityManagerFactory) {
 *      entityManager = entityManagerFactory.createEntityManager();
 *      jpaTestHelper = new JpaTestHelper(entityManager);
 *
 *      personDao = new PersonDao(entityManagerFactory) {
 *          {@literal @}Override
 *           protected EntityManager entityManager() {
 *               return entityManager;
 *           }
 *      };
 *
 *      // additional setup
 *  }
 * </pre>
 * The above assumes your DAO class has a {@code protected} method that tests can override and supply the
 * EntityManager to use. The DAO methods must use the EntityManager returned by this method, otherwise the
 * code being tested in the DAO and the tests will be using different EntityManager instances, which will
 * not work.
 * <p>
 * If you are using Spring with its {@code JpaTransactionManager}, then you can declare a bean of type
 * {@link EntityManager} and defined it as:
 * <pre>
 * {@literal @}Bean
 *  public EntityManager sharedEntityManager(EntityManagerFactory entityManagerFactory) {
 *      return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
 *  }
 * </pre>
 * Then, you can inject this "shared" EntityManager directly into DAOs and tests and it will be
 * automatically used by the DAO and test code without needing to do any of the setup code above.
 */
@Beta
public final class JpaTestHelper {

    /**
     * Return the {@link EntityManager} this helper was created with.
     */
    @Getter
    private final EntityManager entityManager;

    /**
     * Create a new helper with the specified entity manager.
     *
     * @param entityManager the {@link EntityManager} this helper will use
     */
    public JpaTestHelper(EntityManager entityManager) {
        this.entityManager = requireNotNull(entityManager);
    }

    /**
     * Delegates to {@link EntityManager#joinTransaction()}.
     * <p>
     * You should call this if you are handling transactions <em>manually</em> so that tests and the DAOs
     * all participate in the same transaction. If you are using another mechanism to handle transactions,
     * for example using Spring and its Test Context infrastructure using a shared EntityManager, then
     * you may not need to call this.
     *
     * @return this instance, which allows chaining after the constructor
     */
    public JpaTestHelper joinTransaction() {
        entityManager.joinTransaction();
        return this;
    }

    /**
     * Flush the entity manager, effectively forcing JPA to perform database operations. Since this
     * helper assumes tests are executed in a transaction, the database operations are performed
     * <em>but not committed</em>.
     *
     * @see EntityManager#flush()
     */
    public void flushEntityManager() {
        entityManager.flush();
    }

    /**
     * Clears the entity manager.
     *
     * @see EntityManager#clear()
     */
    public void clearEntityManager() {
        entityManager.clear();
    }

    /**
     * Flushes, then clears the entity manager.
     *
     * @see EntityManager#flush()
     * @see EntityManager#clear()
     */
    public void flushAndClearEntityManager() {
        flushEntityManager();
        clearEntityManager();
    }
}
