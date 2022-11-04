package net.playlegend.groupmanager.datastore;

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import net.playlegend.groupmanager.GroupManager;

import java.util.List;
import java.util.logging.Level;

/**
 * This class provides all functionality for accessing and manipulating data. <a
 * href="https://www.spigotmc.org/threads/setup-jpa-hibernate-for-your-minecraft-plugin.397782/">Basic
 * structure inspired by this forum post, but then updated and changed for this project.</a>
 *
 * @param <T> the entity this DAO can work with
 */
public class DAO<T> {

  private final Class<T> entityClass;

  public DAO(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  private EntityManager getEntityManager() {
    return GroupManager.getInstance().getSessionFactory().createEntityManager();
  }

  /**
   * Inserts an object into the data store.
   *
   * @param entity the entity to store
   * @throws DataAccessException if something goes wrong when accessing and manipulating the data
   *     store
   */
  @Transactional
  public synchronized void put(T entity) throws DataAccessException {
    try {
      EntityManager entityManager = this.getEntityManager();
      EntityTransaction transaction = entityManager.getTransaction();
      transaction.begin();
      entityManager.persist(entity);
      transaction.commit();
      entityManager.close();
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * @see DAO#put(T)
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
                GroupManager.getInstance()
                    .getLogger()
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
  protected synchronized T update(T entity) throws DataAccessException {
    try {
      EntityManager entityManager = this.getEntityManager();
      EntityTransaction transaction = entityManager.getTransaction();
      transaction.begin();
      entityManager.merge(entity);
      transaction.commit();
      entityManager.close();
      return entity;
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * @see DAO#update(T)
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
                GroupManager.getInstance()
                    .getLogger()
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
  protected synchronized void delete(T entity) throws DataAccessException {
    try {
      EntityManager entityManager = getEntityManager();
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      entityManager.remove(entityManager.merge(entity));
      entityTransaction.commit();
      entityManager.close();
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * @see DAO#delete(T)
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
                GroupManager.getInstance()
                    .getLogger()
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
  protected synchronized List<T> find(CriteriaAdapter<T> criteriaAdapter)
      throws DataAccessException {
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

      List<Predicate> predicates = criteriaAdapter.applyCriteria(root, criteriaBuilder);

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
      entityManager.close();
      return resultList; // execute the query and return the result list.

      //
      // TODO: Freshen this up -> Make it more readable, possibly move stuff into different
      // sub-methods.
      //
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * @see DAO#find(CriteriaAdapter)
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
                GroupManager.getInstance()
                    .getLogger()
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
  protected synchronized List<T> findAll() throws DataAccessException {
    try {
      return this.find((rootObject, criteriaBuilder) -> Lists.newArrayList());
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  /**
   * @see DAO#findAll()
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

  /**
   * Quickly instantiates a new DAO for a given Entity type. Can be used to access data in a fast
   * and readable way.
   *
   * @param entityClass the entity type class
   * @return a new DAO used for data access and manipulation
   * @param <T> the entity type
   */
  public static <T> DAO<T> forType(Class<T> entityClass) {
    return new DAO<>(entityClass);
  }
}
