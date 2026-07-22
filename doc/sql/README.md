# 数据库初始化脚本

`init.sql` 是后端初始化数据库脚本的文档副本，已与下列原始运行资源进行 SHA-256 一致性校验：

`backend/coding-server/src/main/resources/static/sql/init.sql`

请先在 MySQL 中创建与 `backend/coding-server/.env` 内 `MYSQL_DATABASE` 一致的数据库，再导入：

```powershell
mysql -u <MYSQL_ACCOUNT> -p <MYSQL_DATABASE> < doc/sql/init.sql
```

本目录副本便于查阅和交付；后端原脚本仍保留在资源目录，未被修改或移动。

