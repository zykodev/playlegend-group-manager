package net.playlegend.groupmanager.datastore.wrapper;

import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.model.RankSign;

import java.util.List;

public class RankSignDao {

  /**
   * Collects all known rank signs from the database.
   *
   * @return a list containing all rank signs or null if none are found
   * @throws DataAccessException if there is a communication error
   */
  public static List<RankSign> getAllSigns() throws DataAccessException {
    List<RankSign> matchingSigns =
        Dao.forType(RankSign.class).find((rootObject, criteriaBuilder, output) -> {});
    if (matchingSigns.isEmpty()) return null;
    return matchingSigns;
  }
}
