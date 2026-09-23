package com.schoolmanagement.school.repository;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class SchoolRepository {

    @PersistenceContext
    private EntityManager em;

    public <T> T get(Class<T> type, long id) {
        T row = em.find(type, id);
        if (row == null) throw new ResponseStatusException(NOT_FOUND, "Record not found");
        return row;
    }

    public <T> T lock(Class<T> type, long id) {
        T row = em.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) throw new ResponseStatusException(NOT_FOUND, "Record not found");
        return row;
    }

    public <T> T save(T row) {
        em.persist(row);
        em.flush();
        return row;
    }

    public void delete(Object row) {
        em.remove(row);
    }

    public <T> List<T> query(Class<T> type, String jpql, Object... parameters) {
        var q = em.createQuery(jpql, type);
        for (int i = 0; i < parameters.length; i++) q.setParameter(i + 1, parameters[i]);
        return q.getResultList();
    }

    public long count(String jpql, Object... parameters) {
        return query(Long.class, jpql, parameters).get(0);
    }

    public int sql(String sql, Object... parameters) {
        var q = em.createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i++) q.setParameter(i + 1, parameters[i]);
        return q.executeUpdate();
    }

    public long scalar(String sql, Object... parameters) {
        var q = em.createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i++) q.setParameter(i + 1, parameters[i]);
        return ((Number) q.getSingleResult()).longValue();
    }
}
