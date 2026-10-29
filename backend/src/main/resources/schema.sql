-- ============ 药品进销存 P0 全量建表（SQLite 生产） ============
-- 主键统一 INTEGER PRIMARY KEY AUTOINCREMENT（SQLite rowid 别名，自动自增）

-- 系统：租户（平台级表，无 tenant_id）
CREATE TABLE IF NOT EXISTS sys_tenant (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    status INTEGER NOT NULL DEFAULT 1,
    max_user_count INTEGER,
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    expire_time DATETIME,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 系统：用户
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL DEFAULT 0,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(32),
    email VARCHAR(64),
    status INTEGER NOT NULL DEFAULT 1,
    last_login_time DATETIME,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_tenant_username ON sys_user (tenant_id, username);

-- 系统：角色
CREATE TABLE IF NOT EXISTS sys_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 系统：菜单（平台级表，无 tenant_id）
CREATE TABLE IF NOT EXISTS sys_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    path VARCHAR(128),
    component VARCHAR(128),
    icon VARCHAR(64),
    type INTEGER NOT NULL DEFAULT 2,
    permission VARCHAR(128),
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 系统：用户-角色关联（带 tenant_id，纳入租户隔离）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL
);

-- 系统：角色-菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    menu_id INTEGER NOT NULL
);

-- 基础资料：药品分类
CREATE TABLE IF NOT EXISTS base_drug_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    parent_id INTEGER NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32),
    sort INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 基础资料：药品（min_stock/max_stock 为 P1 预警预留）
CREATE TABLE IF NOT EXISTS base_drug (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    category_id INTEGER,
    generic_name VARCHAR(128) NOT NULL,
    trade_name VARCHAR(128),
    spec VARCHAR(64),
    dosage_form VARCHAR(32),
    manufacturer VARCHAR(128),
    approval_no VARCHAR(64),
    barcode VARCHAR(64),
    storage_condition VARCHAR(64),
    min_stock DECIMAL(18,4),
    max_stock DECIMAL(18,4),
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 基础资料：供应商
CREATE TABLE IF NOT EXISTS base_supplier (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(32),
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    address VARCHAR(255),
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 基础资料：客户
CREATE TABLE IF NOT EXISTS base_customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(32),
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    address VARCHAR(255),
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 基础资料：仓库
CREATE TABLE IF NOT EXISTS base_warehouse (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32),
    address VARCHAR(255),
    manager VARCHAR(64),
    status INTEGER NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 采购：采购单
CREATE TABLE IF NOT EXISTS purchase_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    supplier_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    create_by INTEGER,
    audit_by INTEGER,
    audit_time DATETIME,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 采购：采购单明细
CREATE TABLE IF NOT EXISTS purchase_order_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    order_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    purchase_price DECIMAL(18,4) NOT NULL,
    amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    inbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    batch_no VARCHAR(64),
    expiry_date DATE,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 采购：采购退货单
CREATE TABLE IF NOT EXISTS purchase_return (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    return_no VARCHAR(32) NOT NULL,
    order_id INTEGER,
    supplier_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 采购：采购退货明细
CREATE TABLE IF NOT EXISTS purchase_return_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    return_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    batch_id INTEGER NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    return_price DECIMAL(18,4) NOT NULL,
    amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 销售：销售单
CREATE TABLE IF NOT EXISTS sales_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    customer_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    create_by INTEGER,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 销售：销售单明细
CREATE TABLE IF NOT EXISTS sales_order_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    order_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    sale_price DECIMAL(18,4) NOT NULL,
    discount_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    outbound_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 销售：销售退货单
CREATE TABLE IF NOT EXISTS sales_return (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    return_no VARCHAR(32) NOT NULL,
    order_id INTEGER,
    customer_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 销售：销售退货明细
CREATE TABLE IF NOT EXISTS sales_return_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    return_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    batch_id INTEGER NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    return_price DECIMAL(18,4) NOT NULL,
    amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 库存：实时库存汇总（乐观锁 version）
CREATE TABLE IF NOT EXISTS inv_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_tenant_drug_wh ON inv_stock (tenant_id, drug_id, warehouse_id);

-- 库存：批次库存（乐观锁 version）
CREATE TABLE IF NOT EXISTS inv_stock_batch (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    expiry_date DATE,
    quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    in_date DATE,
    status INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_tenant_drug_wh_no ON inv_stock_batch (tenant_id, drug_id, warehouse_id, batch_no);

-- 库存：流水
CREATE TABLE IF NOT EXISTS inv_flow (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    flow_no VARCHAR(32),
    business_type INTEGER NOT NULL,
    business_no VARCHAR(32),
    drug_id INTEGER NOT NULL,
    warehouse_id INTEGER NOT NULL,
    batch_id INTEGER,
    change_quantity DECIMAL(18,4) NOT NULL,
    before_quantity DECIMAL(18,4),
    after_quantity DECIMAL(18,4),
    operator INTEGER,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 库存：盘点单
CREATE TABLE IF NOT EXISTS inv_stocktake (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    take_no VARCHAR(32) NOT NULL,
    warehouse_id INTEGER NOT NULL,
    status INTEGER NOT NULL DEFAULT 0,
    create_by INTEGER,
    audit_by INTEGER,
    remark VARCHAR(255),
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 库存：盘点明细
CREATE TABLE IF NOT EXISTS inv_stocktake_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id INTEGER NOT NULL,
    take_id INTEGER NOT NULL,
    drug_id INTEGER NOT NULL,
    batch_id INTEGER NOT NULL,
    book_quantity DECIMAL(18,4) NOT NULL,
    actual_quantity DECIMAL(18,4) NOT NULL,
    diff_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);
