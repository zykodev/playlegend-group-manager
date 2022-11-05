package net.playlegend.groupmanager.datastore;

/** Can occur when using the Daos to access the Hibernate backend. */
public class DataAccessException extends Exception {

  public DataAccessException() {
    super();
  }

  public DataAccessException(String message) {
    super(message);
  }

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataAccessException(Throwable cause) {
    super(cause);
  }
}
