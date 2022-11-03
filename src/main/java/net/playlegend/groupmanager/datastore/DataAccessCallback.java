package net.playlegend.groupmanager.datastore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface DataAccessCallback<T> {

  /**
   * This method will be invoked on the callback if the requested operation completes without any errors.
   * @param entity the accessed entity, which could also now hold an updated state.
   */
  void success(@Nullable T entity);

  /**
   * This method will be invoked on the callback if the requested operation completes without any errors.
   * Only invoked if the resulting data set contains more than one entry.
   * @param entities a list of all the entities in the data set
   */
  void multipleSuccess(@Nullable List<T> entities);

  /**
   * This method will be invoked on the callback if the requested operation could not be completed successfully.
   * @param entity the accessed entity
   * @param e the error that occurred
   */
  void error(@Nullable T entity, DataAccessException e);
}
