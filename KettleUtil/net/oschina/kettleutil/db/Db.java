package net.oschina.kettleutil.db;

import cn.benma666.myutils.StringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osjava.sj.loader.SJDataSource;
import org.pentaho.di.core.database.util.DatabaseUtil;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.trans.step.BaseStep;

public class Db extends org.beetl.sql.core.db.Db {

   private static Log log = LogFactory.getLog(Db.class);
   private static String FIND_GENERAL_CONFIG_SQL = "select expand from metl_unify_dict d where d.ocode=? and d.dict_category=?";


   public static Db use(String dbCode) {
      try {
         DataSource e = (new DatabaseUtil()).getNamedDataSource(dbCode);
         return new Db(e, dbCode);
      } catch (KettleException var2) {
         log.error("获取数据库失败:" + dbCode, var2);
         return null;
      }
   }

   public static Db use(BaseStep ku, String dbCode) {
      try {
         DataSource e = (new DatabaseUtil()).getNamedDataSource(dbCode);
         return new Db(e, dbCode);
      } catch (KettleException var3) {
         if(ku != null) {
            ku.logError("获取数据库失败:" + dbCode, var3);
         } else {
            log.error("获取数据库失败:" + dbCode, var3);
         }

         return null;
      }
   }

   public static Db use(JobEntryBase jee, String dbCode) {
      try {
         DataSource e = (new DatabaseUtil()).getNamedDataSource(dbCode);
         return new Db(e, dbCode);
      } catch (KettleException var3) {
         if(jee != null) {
            jee.logError("获取数据库失败:" + dbCode, var3);
         } else {
            log.error("获取数据库失败:" + dbCode, var3);
         }

         return null;
      }
   }

   public Db(DataSource dataSource, String dbCode) {
      super(dbCode, dataSource, getDbtypeByDatasource(dataSource));
   }

   public static String getDbtypeByDatasource(DataSource dataSource) {
      String dbType = null;
      if(dataSource instanceof DruidDataSource) {
         dbType = ((DruidDataSource)dataSource).getDbType();
      } else if(dataSource instanceof SJDataSource) {
         dbType = JdbcUtils.getDbType(((SJDataSource)dataSource).toString().split("::::")[1], (String)null);
      }

      return dbType;
   }

   public JSONObject findGeneralConfig(String configCode) {
      String expand = this.findFirst(FIND_GENERAL_CONFIG_SQL, new Object[]{configCode, "GENERAL_CONFIG"}).getString("expand");
      return StringUtil.isNotBlank(expand)?JSON.parseObject(expand):null;
   }

   public static void closeConn(JobEntryBase jee, Connection conn, PreparedStatement ... preps) {
      PreparedStatement[] e = preps;
      int len$ = preps.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         PreparedStatement p = e[i$];
         if(p != null) {
            try {
               p.close();
            } catch (SQLException var8) {
               jee.logError("关闭预处理游标失败", var8);
            }
         }
      }

      if(conn != null) {
         try {
            conn.close();
         } catch (SQLException var9) {
            if(jee != null) {
               jee.logError("关闭数据库连接失败", var9);
            } else {
               log.error("关闭数据库连接失败", var9);
            }
         }
      }

   }

}
