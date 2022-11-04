package net.playlegend.groupmanager.datastore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Note that this callback is NOT executed in Spigot's main thread.
 * In case you need to perform some synchronized action, please wrap your code using Spigot's scheduler.
 * @param <T> the data type of the accessed entity
 */
public interface DataAccessCallback<T> {

  /**
   * This method will be invoked on the callback if the requested operation completes without any
   * errors.
   *
   * @param entities a list of all involved entities. (Could be empty if for example no entities where found after a search.)
   */
  void success(@Nonnull List<T> entities);

  /**
   * This method will be invoked on the callback if the requested operation could not be completed
   * successfully.
   *
   * @param entities a list of all involved entities. (Could be empty if for example no entities where found after a search or an error occurred.)
   * @param e the error that occurred
   */
  void error(@Nonnull List<T> entities, DataAccessException e);
}
