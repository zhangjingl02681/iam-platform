package com.enterprise.iam.audit;

import com.enterprise.iam.audit.domain.IamAuditEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<IamAuditEvent> search(
            Instant from,
            Instant to,
            UUID actorId,
            String action,
            String eventType,
            String objectType,
            UUID objectId,
            Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<IamAuditEvent> query = cb.createQuery(IamAuditEvent.class);
        Root<IamAuditEvent> root = query.from(IamAuditEvent.class);
        query.where(predicates(cb, root, from, to, actorId, action, eventType, objectType, objectId));
        query.orderBy(orders(cb, root, pageable.getSort()));
        TypedQuery<IamAuditEvent> typed = entityManager.createQuery(query);
        typed.setFirstResult((int) pageable.getOffset());
        typed.setMaxResults(pageable.getPageSize());
        List<IamAuditEvent> content = typed.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<IamAuditEvent> countRoot = countQuery.from(IamAuditEvent.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates(cb, countRoot, from, to, actorId, action, eventType, objectType, objectId));
        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private static Predicate[] predicates(
            CriteriaBuilder cb,
            Root<IamAuditEvent> root,
            Instant from,
            Instant to,
            UUID actorId,
            String action,
            String eventType,
            String objectType,
            UUID objectId) {
        List<Predicate> predicates = new ArrayList<>();
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }
        if (actorId != null) {
            predicates.add(cb.equal(root.get("actorId"), actorId));
        }
        if (StringUtils.hasText(action)) {
            predicates.add(cb.equal(root.get("action"), action));
        }
        if (StringUtils.hasText(eventType)) {
            predicates.add(cb.equal(root.get("eventType"), eventType));
        }
        if (StringUtils.hasText(objectType)) {
            predicates.add(cb.equal(root.get("objectType"), objectType));
        }
        if (objectId != null) {
            predicates.add(cb.equal(root.get("objectId"), objectId));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static List<Order> orders(CriteriaBuilder cb, Root<IamAuditEvent> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        if (sort != null && sort.isSorted()) {
            for (Sort.Order order : sort) {
                orders.add(order.isAscending() ? cb.asc(root.get(order.getProperty())) : cb.desc(root.get(order.getProperty())));
            }
        }
        if (orders.isEmpty()) {
            orders.add(cb.desc(root.get("occurredAt")));
        }
        return orders;
    }
}
