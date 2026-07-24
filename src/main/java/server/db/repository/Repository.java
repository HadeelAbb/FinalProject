package server.db.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface establishing uniform CRUD persistence contract.
 */
public interface Repository<T, ID> {

    /** Find an entity by its Primary Key ID */
    Optional<T> findById(ID id);

    /** Fetch all records in the table */
    List<T> findAll();

    /** Insert a new entity record into the database */
    boolean save(T entity);

    /** Update an existing record in the database */
    boolean update(T entity);

    /** Delete a record by its Primary Key ID */
    boolean deleteById(ID id);
}