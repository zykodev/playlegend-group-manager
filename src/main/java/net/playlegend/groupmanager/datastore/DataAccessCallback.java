package net.playlegend.groupmanager.datastore;

public interface DataAccessCallback<T> {

    void success(T entity);
    void error(T entity, DataAccessException e);

}
