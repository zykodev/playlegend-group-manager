package net.playlegend.groupmanager.datastore;

import net.playlegend.groupmanager.GroupManager;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.Transactional;
import java.util.logging.Level;

public abstract class DAO<T> {

  private EntityManager getEntityManager() {
    return GroupManager.getInstance().getEntityManagerFactory().createEntityManager();
  }

  @Transactional
  public synchronized void put(T entity) throws DataAccessException {
    EntityManager entityManager = this.getEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    entityManager.persist(entity);
    transaction.commit();
    entityManager.close();
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
                    .log(Level.WARNING, "Failed to update entity asynchronously.", e);
              }
            })
        .start();
  }
}
