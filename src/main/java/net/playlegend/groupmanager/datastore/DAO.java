package net.playlegend.groupmanager.datastore;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.transaction.Transactional;
import net.playlegend.groupmanager.GroupManager;

import java.util.List;
import java.util.logging.Level;

public class DAO<T> {

  private final Class<T> entityClass;

  public DAO(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  private EntityManager getEntityManager() {
    return GroupManager.getInstance().getSessionFactory().createEntityManager();
  }

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

  public void putAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                this.put(entity);
                dataAccessCallback.success(entity);
              } catch (DataAccessException e) {
                dataAccessCallback.error(entity, e);
                GroupManager.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Failed to put entity asynchronously.", e);
              }
            })
        .start();
  }

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

  public void updateAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                T newEntity = this.update(entity);
                dataAccessCallback.success(newEntity);
              } catch (DataAccessException e) {
                dataAccessCallback.error(entity, e);
                GroupManager.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Failed to update entity asynchronously.", e);
              }
            })
        .start();
  }

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

  public void deleteAsync(T entity, DataAccessCallback<T> dataAccessCallback) {
    new Thread(
            () -> {
              try {
                this.delete(entity);
                dataAccessCallback.success(entity);
              } catch (DataAccessException e) {
                dataAccessCallback.error(entity, e);
                GroupManager.getInstance()
                        .getLogger()
                        .log(Level.WARNING, "Failed to delete entity asynchronously.", e);
              }
            })
            .start();
  }

  protected synchronized T find(Object id) throws DataAccessException {
    try {
      EntityManager entityManager = getEntityManager();
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      T e = entityManager.find(this.entityClass, id);
      entityTransaction.commit();
      entityManager.close();
      return e;
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  public void findAsync(Object id, DataAccessCallback<T> dataAccessCallback) {
    new Thread(() -> {
      try {
        T newEntity = this.find(id);
        dataAccessCallback.success(newEntity);
      } catch (DataAccessException e) {
        dataAccessCallback.error(null, e);
        GroupManager.getInstance()
                .getLogger()
                .log(Level.WARNING, "Failed to find entity asynchronously.", e);
      }
    }).start();
  }

  protected synchronized List<T> findAll() throws DataAccessException {
    try {
      EntityManager entityManager = getEntityManager();
      EntityTransaction entityTransaction = entityManager.getTransaction();
      entityTransaction.begin();
      CriteriaQuery<T> cq = this.getEntityManager().getCriteriaBuilder().createQuery(entityClass);
      cq.select(cq.from(entityClass));
      List<T> e = this.getEntityManager().createQuery(cq).getResultList();
      entityTransaction.commit();
      entityManager.close();
      return e;
    } catch (Exception e) {
      throw new DataAccessException(e);
    }
  }

  public void findAllAsync(DataAccessCallback<T>dataAccessCallback) {
    new Thread(() -> {
      try {
        dataAccessCallback.multipleSuccess(this.findAll());
      } catch (DataAccessException e) {
        dataAccessCallback.error(null, e);
      }
    }).start();
  }

}
