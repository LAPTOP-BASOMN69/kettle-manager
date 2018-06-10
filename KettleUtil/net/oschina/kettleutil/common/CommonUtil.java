package net.oschina.kettleutil.common;

import com.alibaba.fastjson.JSONObject;
import net.oschina.kettleutil.db.Db;
import net.oschina.mytuils.KettleUtils;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;

public class CommonUtil {

   public static DatabaseMeta getOrCreateDB(String dbCode) throws KettleException {
      ObjectId dbId = null;
      Repository repository = KettleUtils.getInstanceRep();
      dbId = repository.getDatabaseID(dbCode);
      if(dbId == null) {
         JSONObject metlDb = Db.use("metl").findFirst("select * from metl_database db where db.ocode=?", new Object[]{dbCode});
         DatabaseMeta dataMeta = new DatabaseMeta(dbCode, KettleUtils.dbTypeToKettle(metlDb.getString("type")), DatabaseMeta.dbAccessTypeCode[4], (String)null, dbCode, (String)null, (String)null, (String)null);
         KettleUtils.saveRepositoryElement(dataMeta);
         dbId = repository.getDatabaseID(dbCode);
      }

      return repository.loadDatabaseMeta(dbId, (String)null);
   }
}
