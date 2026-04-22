create table users (
    id uuid primary key,
    name varchar(160) not null,
    email varchar(180) not null unique,
    password_hash varchar(255),
    role varchar(32) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table stops (
    id uuid primary key,
    code varchar(40) not null unique,
    name varchar(160) not null,
    address varchar(255) not null,
    latitude numeric(10,6) not null,
    longitude numeric(10,6) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table routes (
    id uuid primary key,
    name varchar(160) not null unique,
    status varchar(32) not null,
    origin_stop_id uuid not null references stops(id),
    destination_stop_id uuid not null references stops(id),
    geometry text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table route_stops (
    id uuid primary key,
    route_id uuid not null references routes(id) on delete cascade,
    stop_id uuid not null references stops(id),
    stop_order integer not null,
    constraint uk_route_stop_order unique (route_id, stop_order),
    constraint uk_route_stop_stop unique (route_id, stop_id)
);

create table buses (
    id uuid primary key,
    plate varchar(40) not null unique,
    code varchar(40) not null unique,
    capacity integer not null,
    route_id uuid references routes(id),
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table fares (
    id uuid primary key,
    name varchar(160) not null,
    amount numeric(12,2) not null,
    valid_from date not null,
    valid_to date not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table payments (
    id uuid primary key,
    user_id uuid not null references users(id),
    bus_id uuid not null references buses(id),
    amount numeric(12,2) not null,
    date timestamp with time zone not null,
    status varchar(32) not null,
    method varchar(32) not null,
    external_reference varchar(160),
    reversal_reason varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_buses_route on buses(route_id);
create index idx_route_stops_route on route_stops(route_id);
create index idx_payments_date on payments(date);
create index idx_payments_user on payments(user_id);
create index idx_payments_bus on payments(bus_id);
