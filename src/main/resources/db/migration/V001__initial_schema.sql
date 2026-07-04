CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    budget NUMERIC(14, 2),
    icon_name VARCHAR(64) NOT NULL,
    color_hex INTEGER NOT NULL,
    template_recurrence VARCHAR(24),
    source_template_id UUID,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shopping_lists_owner ON shopping_lists(owner_id);
CREATE INDEX idx_shopping_lists_owner_archived_template ON shopping_lists(owner_id, archived, template_recurrence);

CREATE TABLE shopping_items (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    quantity INTEGER NOT NULL,
    unit VARCHAR(16) NOT NULL,
    category_name VARCHAR(120) NOT NULL,
    store_name VARCHAR(160),
    planned_price NUMERIC(14, 2),
    actual_price NUMERIC(14, 2),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    note VARCHAR(600) NOT NULL DEFAULT '',
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shopping_items_list_sort ON shopping_items(list_id, sort_order);

CREATE TABLE shopping_categories (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(160) NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    icon_name VARCHAR(64) NOT NULL,
    color_hex INTEGER NOT NULL,
    fixed BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_category_owner_normalized UNIQUE(owner_id, normalized_name)
);

CREATE INDEX idx_shopping_categories_owner_sort ON shopping_categories(owner_id, sort_order);

CREATE TABLE item_catalog_entries (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(160) NOT NULL,
    name VARCHAR(180) NOT NULL,
    category_name VARCHAR(120) NOT NULL,
    store_name VARCHAR(160),
    planned_price NUMERIC(14, 2),
    actual_price NUMERIC(14, 2),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    quantity INTEGER NOT NULL,
    unit VARCHAR(16) NOT NULL,
    note VARCHAR(600) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_item_catalog_owner ON item_catalog_entries(owner_id, archived, favorite);

CREATE TABLE purchase_history_entries (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(160) NOT NULL,
    source_list_id UUID NOT NULL,
    source_template_id UUID,
    name VARCHAR(160) NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    budget NUMERIC(14, 2),
    planned_total NUMERIC(14, 2) NOT NULL,
    actual_total NUMERIC(14, 2) NOT NULL,
    budget_delta NUMERIC(14, 2),
    plan_delta NUMERIC(14, 2),
    purchased_item_count INTEGER NOT NULL,
    total_item_count INTEGER NOT NULL,
    missing_actual_price_count INTEGER NOT NULL,
    sections_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_purchase_history_owner_finished ON purchase_history_entries(owner_id, finished_at DESC);

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    status TEXT,
    completion_attempts INTEGER,
    last_resubmission_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication(completion_date);

CREATE TABLE IF NOT EXISTS event_publication_archive (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    status TEXT,
    completion_attempts INTEGER,
    last_resubmission_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_archive_serialized_event_hash_idx ON event_publication_archive USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_archive_by_completion_date_idx ON event_publication_archive(completion_date);
