package com.devbridge.logicore.security;

import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.Warehouse;

/** What part of the data the current user may see. */
public final class Scope {

    public enum Level { ALL, REGION, WAREHOUSE, NONE }

    private static final Scope ALL = new Scope(Level.ALL, null, null);
    private static final Scope NONE = new Scope(Level.NONE, null, null);

    private final Level level;
    private final Long regionId;
    private final Long warehouseId;

    private Scope(Level level, Long regionId, Long warehouseId) {
        this.level = level;
        this.regionId = regionId;
        this.warehouseId = warehouseId;
    }

    public static Scope all() { return ALL; }
    public static Scope none() { return NONE; }
    public static Scope region(Long regionId) { return new Scope(Level.REGION, regionId, null); }
    public static Scope warehouse(Long warehouseId) { return new Scope(Level.WAREHOUSE, null, warehouseId); }

    public Level getLevel() { return level; }
    public Long getRegionId() { return regionId; }
    public Long getWarehouseId() { return warehouseId; }

    public boolean isAll() { return level == Level.ALL; }

    /** True when the shipment starts or ends inside this scope. Call inside a transaction. */
    public boolean canSee(Shipment shipment) {
        switch (level) {
            case ALL:
                return true;
            case REGION:
                return inRegion(shipment.getOriginWarehouse()) || inRegion(shipment.getDestinationWarehouse());
            case WAREHOUSE:
                return isWarehouse(shipment.getOriginWarehouse()) || isWarehouse(shipment.getDestinationWarehouse());
            default:
                return false;
        }
    }

    private boolean inRegion(Warehouse warehouse) {
        return warehouse != null && warehouse.getRegion() != null
                && regionId.equals(warehouse.getRegion().getId());
    }

    private boolean isWarehouse(Warehouse warehouse) {
        return warehouse != null && warehouseId.equals(warehouse.getId());
    }
}