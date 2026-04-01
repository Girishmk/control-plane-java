-- V1__create_orders_schema.sql
-- Initial schema for order-service

CREATE TABLE orders (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL CHECK (status IN (
                      'PENDING','CONFIRMED','PROCESSING',
                      'SHIPPED','DELIVERED','CANCELLED','REFUNDED')),
    total_amount  NUMERIC(12,2),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku          VARCHAR(64)  NOT NULL,
    product_name VARCHAR(256) NOT NULL,
    quantity     INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(12,2) NOT NULL,
    line_total   NUMERIC(12,2) NOT NULL
);

-- Indexes for common query patterns
CREATE INDEX idx_orders_user_id  ON orders(user_id);
CREATE INDEX idx_orders_status   ON orders(status);
CREATE INDEX idx_orders_created  ON orders(created_at DESC);
CREATE INDEX idx_order_items_order ON order_items(order_id);
