package net.oschina.kettleutil.jobentry;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.oschina.kettleutil.jobentry.JobEntryKettleUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class JobEntryKettleUtilRunBase {

   protected Log log = LogFactory.getLog(this.getClass());
   protected JSONObject configInfo;
   protected JobEntryKettleUtil jeku;


   protected abstract boolean run() throws Exception;

   public String getDefaultConfigInfo() throws Exception {
      return "{}";
   }

   public JSONObject getConfigInfo() {
      return this.configInfo;
   }

   public void setConfigInfo(JSONObject configInfo) {
      this.configInfo = configInfo;
   }

   public JobEntryKettleUtil getJeku() {
      return this.jeku;
   }

   public void setJeku(JobEntryKettleUtil jeku) {
      this.jeku = jeku;

      try {
         this.configInfo = JSON.parseObject(jeku.environmentSubstitute(jeku.getConfigInfo()));
      } catch (Exception var3) {
         this.log.debug("配置信息不能转换为JSON对象", var3);
         this.configInfo = new JSONObject();
      }

   }
}
