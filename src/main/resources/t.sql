CREATE TABLE IF NOT EXISTS flog
(
    id INTEGER PRIMARY KEY AUTOINCREMENT DEFAULT 0 NOT NULL,
    _time INTEGER NOT NULL,
    _level INTEGER NOT NULL,
    _st TEXT,
    _msg TEXT,
    _add TEXT
);
CREATE UNIQUE INDEX flog_id_uindex ON flog (id);




CREATE TABLE IF NOT EXISTS stelement
(
    id int PRIMARY KEY AUTOINCREMENT,
    fileName TEXT NOT NULL,
    lineNumber int NOT NULL,
    className TEXT NOT NULL,
    methodName TEXT NOT NULL,
    isNative int NOT NULL
);