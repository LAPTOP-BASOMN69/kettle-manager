tab_comments
===
* 查询表的注释
select *
  from dba_tab_comments t
 where t.OWNER = #owner#
   and t.TABLE_NAME = #tableName#

cols_info
===
* 查询表的列信息
select t.*,
       col.DATA_TYPE,
       col.DATA_LENGTH,
       col.DATA_DEFAULT,
       p.constraint_name
  from dba_col_comments t
  left join dba_tab_cols col
    on col.owner = t.owner
   and col.TABLE_NAME = t.table_name
   and col.COLUMN_NAME = t.column_name
  left join (select a.constraint_name, a.column_name, b.owner, b.table_name
               from dba_cons_columns a, dba_constraints b
              where a.constraint_name = b.constraint_name
                and b.constraint_type = 'P'
                and a.owner = b.owner) p
    on p.owner = t.owner
   and p.TABLE_NAME = t.table_name
   and p.COLUMN_NAME = t.column_name
 where t.OWNER = #owner#
   and t.TABLE_NAME = #tableName#

