-- V1 baseline: the schema as of release v1.2.
-- Fresh databases run this script. An existing database is marked as already at version 1.

create table regions (
    id bigserial not null,
    active boolean not null,
    code varchar(20) not null,
    name varchar(80) not null,
    primary key (id)
);

create table warehouses (
    id bigserial not null,
    active boolean not null,
    city varchar(80),
    code varchar(20) not null,
    country varchar(80),
    name varchar(120) not null,
    region_id bigint not null,
    primary key (id)
);

create table app_users (
    id bigserial not null,
    created_at timestamp not null,
    email varchar(160) not null,
    enabled boolean not null,
    full_name varchar(120) not null,
    password_hash varchar(100) not null,
    role varchar(30) not null,
    region_id bigint,
    warehouse_id bigint,
    primary key (id)
);

create table audit_logs (
    id bigserial not null,
    action varchar(40) not null,
    created_at timestamp not null,
    detail varchar(500),
    entity_id varchar(60) not null,
    entity_type varchar(40) not null,
    performed_by varchar(120) not null,
    primary key (id)
);

create table customers (
    id bigserial not null,
    active boolean not null,
    address varchar(255),
    contact_person varchar(120),
    created_at timestamp not null,
    email varchar(160),
    name varchar(120) not null,
    phone varchar(30),
    primary key (id)
);

create table shipments (
    id bigserial not null,
    created_at timestamp not null,
    customer_name varchar(120) not null,
    destination varchar(120) not null,
    mode varchar(30) not null,
    origin varchar(120) not null,
    reference_number varchar(40),
    status varchar(30) not null,
    tracking_number varchar(40) not null,
    updated_at timestamp not null,
    weight numeric(12, 2) not null,
    customer_id bigint,
    destination_warehouse_id bigint,
    origin_warehouse_id bigint,
    primary key (id)
);

create table shipment_status_history (
    id bigserial not null,
    remarks varchar(500),
    status varchar(30) not null,
    updated_at timestamp not null,
    updated_by varchar(80) not null,
    shipment_id bigint not null,
    primary key (id)
);

create table storage_locations (
    id bigserial not null,
    bin varchar(20) not null,
    rack varchar(20) not null,
    zone varchar(20) not null,
    warehouse_id bigint,
    primary key (id)
);

create table warehouse_receipts (
    id bigserial not null,
    condition varchar(30) not null,
    issue_cleared_at timestamp,
    issue_cleared_by varchar(80),
    issue_note varchar(300),
    received_at timestamp not null,
    received_by varchar(80) not null,
    weight_checked numeric(12, 2) not null,
    shipment_id bigint not null,
    primary key (id)
);

create table freight_assignments (
    id bigserial not null,
    assigned_at timestamp not null,
    shipment_id bigint not null,
    storage_location_id bigint not null,
    primary key (id)
);

create table dispatches (
    id bigserial not null,
    destination varchar(120) not null,
    dispatched_at timestamp not null,
    vehicle_number varchar(40) not null,
    shipment_id bigint not null,
    primary key (id)
);

create table customs_clearances (
    id bigserial not null,
    cleared_at timestamp not null,
    cleared_by varchar(80) not null,
    customs_reference varchar(60) not null,
    note varchar(300),
    shipment_id bigint not null,
    primary key (id)
);

-- Unique rules
alter table regions add constraint uk_regions_code unique (code);
alter table warehouses add constraint uk_warehouses_code unique (code);
alter table app_users add constraint uk_app_users_email unique (email);
alter table shipments add constraint uk_shipments_tracking_number unique (tracking_number);
alter table storage_locations add constraint uk_storage_location unique (warehouse_id, zone, rack, bin);
alter table customs_clearances add constraint uk_customs_shipment unique (shipment_id);

-- Indexes
create index idx_audit_entity on audit_logs (entity_type, entity_id);
create index idx_audit_created on audit_logs (created_at);

-- Foreign keys
alter table warehouses add constraint fk_warehouses_region
    foreign key (region_id) references regions;
alter table app_users add constraint fk_app_users_region
    foreign key (region_id) references regions;
alter table app_users add constraint fk_app_users_warehouse
    foreign key (warehouse_id) references warehouses;
alter table shipments add constraint fk_shipments_customer
    foreign key (customer_id) references customers;
alter table shipments add constraint fk_shipments_origin_warehouse
    foreign key (origin_warehouse_id) references warehouses;
alter table shipments add constraint fk_shipments_destination_warehouse
    foreign key (destination_warehouse_id) references warehouses;
alter table shipment_status_history add constraint fk_history_shipment
    foreign key (shipment_id) references shipments;
alter table storage_locations add constraint fk_storage_locations_warehouse
    foreign key (warehouse_id) references warehouses;
alter table warehouse_receipts add constraint fk_receipts_shipment
    foreign key (shipment_id) references shipments;
alter table freight_assignments add constraint fk_assignments_shipment
    foreign key (shipment_id) references shipments;
alter table freight_assignments add constraint fk_assignments_location
    foreign key (storage_location_id) references storage_locations;
alter table dispatches add constraint fk_dispatches_shipment
    foreign key (shipment_id) references shipments;
alter table customs_clearances add constraint fk_customs_shipment
    foreign key (shipment_id) references shipments;