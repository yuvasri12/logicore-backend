package com.devbridge.logicore.repository;

import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.security.Scope;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

/** The building blocks of the shipment search: each one is a single, readable rule. */
public final class ShipmentSpecs {

    private ShipmentSpecs() {
    }

    /** Only shipments the user may see: all, their region, their warehouse, or nothing. */
    public static Specification<Shipment> visibleTo(final Scope scope) {
        return (root, query, cb) -> {
            switch (scope.getLevel()) {
                case ALL:
                    return cb.conjunction();
                case REGION: {
                    Join<Shipment, Warehouse> origin = root.join("originWarehouse", JoinType.LEFT);
                    Join<Shipment, Warehouse> destination = root.join("destinationWarehouse", JoinType.LEFT);
                    return cb.or(
                            cb.equal(origin.get("region").get("id"), scope.getRegionId()),
                            cb.equal(destination.get("region").get("id"), scope.getRegionId()));
                }
                case WAREHOUSE: {
                    Join<Shipment, Warehouse> origin = root.join("originWarehouse", JoinType.LEFT);
                    Join<Shipment, Warehouse> destination = root.join("destinationWarehouse", JoinType.LEFT);
                    return cb.or(
                            cb.equal(origin.get("id"), scope.getWarehouseId()),
                            cb.equal(destination.get("id"), scope.getWarehouseId()));
                }
                default:
                    return cb.disjunction();
            }
        };
    }

    public static Specification<Shipment> hasStatus(final ShipmentStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Shipment> hasMode(final ShipmentMode mode) {
        if (mode == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("mode"), mode);
    }

    /** Text search over tracking number, customer, places, warehouse names and reference number. */
    public static Specification<Shipment> matches(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        final String pattern = "%" + escape(text.trim().toLowerCase()) + "%";
        return (root, query, cb) -> {
            Join<Shipment, Warehouse> origin = root.join("originWarehouse", JoinType.LEFT);
            Join<Shipment, Warehouse> destination = root.join("destinationWarehouse", JoinType.LEFT);
            return cb.or(
                    like(cb, root.<String>get("trackingNumber"), pattern),
                    like(cb, root.<String>get("customerName"), pattern),
                    like(cb, root.<String>get("origin"), pattern),
                    like(cb, root.<String>get("destination"), pattern),
                    like(cb, root.<String>get("referenceNumber"), pattern),
                    like(cb, origin.<String>get("name"), pattern),
                    like(cb, destination.<String>get("name"), pattern));
        };
    }

    /**
     * Loads both warehouses in the same query as the shipments, so building the response does
     * not need one extra query per row. Skipped for the count query that paging runs.
     */
    public static Specification<Shipment> withWarehouses() {
        return (root, query, cb) -> {
            Class<?> type = query.getResultType();
            if (type != Long.class && type != long.class) {
                root.fetch("originWarehouse", JoinType.LEFT);
                root.fetch("destinationWarehouse", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> expression, String pattern) {
        return cb.like(cb.lower(expression), pattern, '\\');
    }

    /** Makes %, _ and \ typed by the user match themselves instead of acting as wildcards. */
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}