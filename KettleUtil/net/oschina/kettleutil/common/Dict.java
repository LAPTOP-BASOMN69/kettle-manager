package net.oschina.kettleutil.common;

import cn.benma666.myutils.StringUtil;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import net.oschina.kettleutil.db.Db;

public class Dict {

   public static String dictCategoryToSql(String dictCategory) {
      String defaultVal = "select ocode ID,oname CN from metl_unify_dict where dict_category=\'" + dictCategory + "\' and is_disable=\'" + "0" + "\' order by oorder asc;ds=metl";
      String result = dictCategory;
      if(StringUtil.isNotBlank(dictCategory) && !dictCategory.toLowerCase().startsWith("select")) {
         String expand = Db.use("metl").queryStr("select expand from metl_unify_dict t where t.dict_category=\'DICT_CATEGORY\' and is_disable=? and t.ocode=?", new Object[]{"0", dictCategory});

         try {
            result = JSONObject.parseObject(expand).getString("sql");
            if(StringUtil.isBlank(result)) {
               result = defaultVal;
            }
         } catch (Exception var5) {
            result = defaultVal;
         }
      }

      return result;
   }

   public static List dictList(String dictCategory) {
      String expStr = dictCategoryToSql(dictCategory);
      String[] dict = parseDictExp(expStr);
      List result = Db.use(dict[1]).find(dict[0], new Object[0]);
      return result;
   }

   public static String dictValue(String dictCategory, String key) {
      String expStr = dictCategoryToSql(dictCategory);
      String[] dict = parseDictExp(expStr);
      String sql = "select cn from (" + dict[0] + ") t where t.id=?";
      String result = Db.use(dict[1]).queryStr(sql, new Object[]{key});
      if(StringUtil.isBlank(result)) {
         result = key;
      }

      return result;
   }

   public static List dictObjList(String dictCategory) {
      List result = Db.use("metl").find("select * from metl_unify_dict where dict_category=? and is_disable=? order by oorder asc", new Object[]{dictCategory, "0"});
      return result;
   }

   public static JSONObject dictObj(String dictCategory, String key) {
      JSONObject result = Db.use("metl").findFirst("select * from metl_unify_dict where dict_category=? and is_disable=? and ocode=?", new Object[]{dictCategory, "0", key});
      return result;
   }

   public static String[] parseDictExp(String exp) {
      if(StringUtil.isBlank(exp)) {
         return null;
      } else {
         String[] result = new String[2];
         String[] strs = exp.split(";");
         result[0] = strs[0];
         if(strs.length > 1) {
            result[1] = strs[1].substring(3);
         } else {
            result[1] = "metl";
         }

         return result;
      }
   }
}
