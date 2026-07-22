-- Existing databases should run this migration once before enabling the cleanup job.
ALTER TABLE app
    ADD INDEX idx_deleted_cleanup (isDelete, updateTime, id);
