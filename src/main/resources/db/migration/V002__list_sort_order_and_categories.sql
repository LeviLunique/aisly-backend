ALTER TABLE shopping_lists ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE shopping_lists ADD COLUMN categories_json TEXT;

CREATE INDEX idx_shopping_lists_owner_sort ON shopping_lists(owner_id, sort_order);
