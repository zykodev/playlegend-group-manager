package net.playlegend.groupmanager.datastore;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/**
 * Adapter interface used to apply filter criteria in a search query.
 * @param <T> the entity type the search is being performed on.
 */
public interface CriteriaAdapter<T> {

    /**
     * This adapter can be used to apply filter criteria to a search query.
     * Example:
     * <ul>
     *     Predicate filterPriorityEquals92 = criteriaBuilder.equal(rootObject.get(Group_.PRIORITY), 92);
     * </ul>
     * Add this predicate to the returned list of predicates to filter for all groups with a priority of 92.
     *
     * @param rootObject the root object virtually selecting everything
     * @param criteriaBuilder the criteria builder used to create filtering predicates
     * @return a list containing all predicates to be applied when filtering
     */
    List<Predicate> applyCriteria(Root<T> rootObject, CriteriaBuilder criteriaBuilder);

}
