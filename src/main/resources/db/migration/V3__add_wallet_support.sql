create table wallet_transactions (
    id uuid primary key,
    user_id uuid not null references users(id),
    amount numeric(12,2) not null,
    type varchar(32) not null,
    status varchar(32) not null,
    method varchar(32),
    payment_id uuid references payments(id),
    date timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_wallet_transactions_user on wallet_transactions(user_id);
create index idx_wallet_transactions_date on wallet_transactions(date);
create index idx_wallet_transactions_payment on wallet_transactions(payment_id);
