package net.playlegend.groupmanager.datastore;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import net.playlegend.groupmanager.GroupManagerPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Provides all functionality for accessing and manipulating data. <br>
 * <a
 * href="https://www.spigotmc.org/threads/setup-jpa-hibernate-for-your-minecraft-plugin.397782/">Basic
 * structure inspired by this forum post, but then updated and changed for this project.</a>
 *
 * @param <T> the entity this DAO can work with
 */
public class Dao<T> {

  private final Object lockObject = new Object();
  private static final Object staticLockObject = new Object();
  private final Class<T> entityClass;
  private EntityManager entityManager;

  /**
   * Constructs a new DAO. Note that the resulting DAO is not cached automatically, so it is
   * generally advised to use {@code Dao.fromType(Class<T>)} in order to prevent exhausting the
   * connection pool.
   *
   * @param entityClass the dao type
   */
  public Dao(Class<T> entityClass) {
    this.entityClass = entityClass;
    this.entityManager = this.createEntityManager();
  }

  private EntityManager createEntityManager() {
    return GroupManagerPlugin.getInstance().getSessionFactory().createEntityManager();
  }

  private EntityManager getEntityManager() {
    if (this.entityManager.isOpen()) {
      return this.entityManager;
    }
    return (this.entityManager = this.createEntityManager());
  }

  /**
   * Inserts an object into the data store.
   *
   * @param entity the entity to store
   * @throws DataAccessException if something goes wrong when accessing and manipulating the data
   *     store
   */
  @Transactional
  public void put(T entity) throws DataAccessException {
    synchronized (this.lockObject) {
      try {
        EntityManager entityManager = this.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(entity);
        transaction.commit();
        GroupManagerPlugin.getInstance().rebuildEverything();
      } catch (Exception e) {
        throw new DataAccessException(e);
      }
    }
  }

  /**
   * @see Dao#put(T)
   * @see DataAccessCallback
   * @param entity the entity to store
   * @param dataAccessCallback the callback to handle the resulting data or eventual errors
   */
  public void putAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                this.put(entity);
                dataAccessCallback.success(Lists.newArrayList(entity));
              } catch (DataAccessException e) {
                dataAccessCallback.error(Lists.newArrayList(entity), e);
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to put entity asynchronously.", e);
              }
            })
        .start();
  }

  /**
   * Updates an object in the data store.
   *
   * @param entity the entity to update containing up-to-date variables
   * @return the entity given
   * @throws DataAccessException if something goes wrong when manipulating the data
   */
  @Transactional
  public T update(T entity) throws DataAccessException {
    synchronized (this.lockObject) {
      try {
        EntityManager entityManager = this.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.merge(entity);
        transaction.commit();
        GroupManagerPlugin.getInstance().rebuildEverything();
        return entity;
      } catch (Exception e) {
        throw new DataAccessException(e);
      }
    }
  }

  /**
   * @see Dao#update(T)
   * @see DataAccessCallback
   * @param entity the entity to update containing up-to-date variables
   * @param dataAccessCallback the callback to handle the resulting data or eventual errors
   */
  public void updateAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                T newEntity = this.update(entity);
                dataAccessCallback.success(Lists.newArrayList(newEntity));
              } catch (DataAccessException e) {
                dataAccessCallback.error(Lists.newArrayList(entity), e);
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to update entity asynchronously.", e);
              }
            })
        .start();
  }

  /**
   * Deletes an object from the data store.
   *
   * @param entity the entity to delete
   * @throws DataAccessException if something goes wrong when accessing or manipulating the data
   */
  @Transactional
  public void delete(T entity) throws DataAccessException {
    synchronized (this.lockObject) {
      try {
        EntityManager entityManager = getEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        entityManager.remove(entityManager.merge(entity));
        entityTransaction.commit();
        GroupManagerPlugin.getInstance().rebuildEverything();
      } catch (Exception e) {
        throw new DataAccessException(e);
      }
    }
  }

  /**
   * @see Dao#delete(T)
   * @see DataAccessCallback
   * @param entity the entity to delete
   * @param dataAccessCallback the callback to handle the resulting data or eventual errors
   */
  public void deleteAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                this.delete(entity);
                dataAccessCallback.success(Lists.newArrayList(entity));
              } catch (DataAccessException e) {
                dataAccessCallback.error(Lists.newArrayList(entity), e);
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to delete entity asynchronously.", e);
              }
            })
        .start();
  }

  /**
   * Searches for objects fulfilling the given criteria, puts them into a list and returns them.
   *
   * @param criteriaAdapter the criteria adapter used to supply a set of predicates
   * @return a list containing all objects fulfilling the criteria
   * @throws DataAccessException if something goes wrong when accessing the data store
   */
  public List<T> find(CriteriaAdapter<T> criteriaAdapter) throws DataAccessException {
    synchronized (this.lockObject) {
      try {
        EntityManager entityManager = getEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();
        entityTransaction.begin();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.entityClass);
        Root<T> root =
            criteriaQuery.from(
                this.entityClass); // root now virtually selects every object, and we can filter on
        // this root object.

        List<Predicate> predicates = Lists.newArrayList();
        criteriaAdapter.applyCriteria(root, criteriaBuilder, predicates);

        CriteriaQuery<T> query;
        if (predicates.isEmpty()) {
          query = criteriaQuery.select(root); // no need to filter, as there are no criteria.
        } else {
          query =
              criteriaQuery
                  .select(root)
                  .where(predicates.toArray(Predicate[]::new)); // filter on the root object.
        }

        TypedQuery<T> result = entityManager.createQuery(query);
        List<T> resultList = result.getResultList();
        entityTransaction.commit();
        return resultList; // execute the query and return the result list.
      } catch (Exception e) {
        throw new DataAccessException(e);
      }
    }
  }

  /**
   * @see Dao#find(CriteriaAdapter)
   * @see DataAccessCallback
   * @param criteriaAdapter the criteria adapter used to supply a set of predicates
   * @param dataAccessCallback the callback to handle the resulting data or eventual errors
   */
  public void findAsync(
      CriteriaAdapter<T> criteriaAdapter, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                List<T> foundEntities = this.find(criteriaAdapter);
                dataAccessCallback.success(foundEntities);
              } catch (DataAccessException e) {
                dataAccessCallback.error(Lists.newArrayList(), e);
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to find entity asynchronously.", e);
              }
            })
        .start();
  }

  /**
   * Quickly returns all objects stored of the given type.
   *
   * @return all objects of the given type, in a list
   * @throws DataAccessException if something goes wrong when accessing the data
   */
  public List<T> findAll() throws DataAccessException {
    synchronized (this.lockObject) {
      try {
        return this.find((rootObject, criteriaBuilder, output) -> {});
      } catch (Exception e) {
        throw new DataAccessException(e);
      }
    }
  }

  /**
   * @see Dao#findAll()
   * @see DataAccessCallback
   * @param dataAccessCallback the callback to handle the resulting data or eventual errors
   */
  public void findAllAsync(DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                dataAccessCallback.success(this.findAll());
              } catch (DataAccessException e) {
                dataAccessCallback.error(Lists.newArrayList(), e);
              }
            })
        .start();
  }

  private static final Map<Class<?>, Dao> DAO_CACHE_MAP =
      Collections.synchronizedMap(Maps.newHashMap());

  /**
   * Quickly instantiates a new DAO or retrieves a cached DAO for a given Entity type. Can be used
   * to access data in a fast and readable way.
   *
   * @param entityClass the entity type class
   * @return a new DAO used for data access and manipulation
   * @param <T> the entity type
   */
  public static <T> Dao<T> forType(Class<T> entityClass) {
    synchronized (Dao.staticLockObject) {
      Dao<T> dao = Dao.DAO_CACHE_MAP.get(entityClass);
      if (dao == null) {
        dao = new Dao<>(entityClass);
        Dao.DAO_CACHE_MAP.put(entityClass, dao);
      }
      return dao;
    }
  }

  /** Destroys all cached DAOs in order to invalidate caches. */
  public static void destroyDaoCache() {
    synchronized (Dao.staticLockObject) {
      Dao.DAO_CACHE_MAP.values().forEach(Dao::destroyDao);
      Dao.DAO_CACHE_MAP.clear();
    }
  }

  /** Shuts down this DAO, clearing its cache. */
  public void destroyDao() {
    synchronized (this.lockObject) {
      this.getEntityManager().close();
    }
  }
}
