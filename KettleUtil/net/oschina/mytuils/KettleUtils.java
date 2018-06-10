package net.oschina.mytuils;

import cn.benma666.myutils.JdbcUtil;
import cn.benma666.myutils.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.oschina.mytuils.TransPreviewUtil;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.ProgressMonitorListener;
import org.pentaho.di.core.KettleClientEnvironment.ClientType;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleSecurityException;
import org.pentaho.di.core.parameters.NamedParams;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.job.JobEntryJob;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.BaseRepositoryMeta;
import org.pentaho.di.repository.LongObjectId;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryElementInterface;
import org.pentaho.di.repository.StringObjectId;
import org.pentaho.di.repository.filerep.KettleFileRepository;
import org.pentaho.di.repository.filerep.KettleFileRepositoryMeta;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepositoryMeta;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.jobexecutor.JobExecutorMeta;
import org.pentaho.di.trans.steps.transexecutor.TransExecutorMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KettleUtils {

   public static Logger log = LoggerFactory.getLogger(KettleUtils.class);
   private static Repository repository;
   private static TransMeta transMetaTemplate;
   private static JobMeta jobMetaTemplate;
   private static Map repMap = new HashMap();


   public static Repository getInstanceRep() throws KettleException {
      if(repository != null) {
         return repository;
      } else {
         throw new KettleException("没有初始化资源库");
      }
   }

   public static Repository use(String repId) {
      return (Repository)repMap.get(repId);
   }

   public static String dbTypeToKettle(String dbType) {
      return "oracle".equals(dbType)?"Oracle":("mysql".equals(dbType)?"mysql":null);
   }

   public static void connectKettle(String name, String type, String kuser, String kpass) throws Exception {
      destroy();
      createDBRepByJndi(name, dbTypeToKettle(type), name);
      connect(kuser, kpass);
   }

   public static void connectKettle(String name, String url, String user, String pass, String kuser, String kpass) throws Exception {
      destroy();
      createDBRep(name, url, user, pass);
      connect(kuser, kpass);
   }

   public static void connectKettle(String name, String type, String access, String host, String db, String port, String user, String pass, JSONObject params, String kuser, String kpass) throws Exception {
      destroy();
      createDBRep(name, dbTypeToKettle(type), access, host, db, port, user, pass, params);
      connect(kuser, kpass);
   }

   public static Repository createFileRep(String id, String repName, String description, String baseDirectory) throws KettleException {
      initEnv();
      if(!KettleEnvironment.isInitialized()) {
         KettleEnvironment.init();
      }

      KettleFileRepositoryMeta fileRepMeta = new KettleFileRepositoryMeta(id, repName, description, baseDirectory);
      return createRep(fileRepMeta, id, repName, description);
   }

   public static Repository createDBRepByJndi(String name, String type, String db) throws KettleException {
      return createDBRep(name, type, DatabaseMeta.dbAccessTypeCode[4], (String)null, db, (String)null, (String)null, (String)null, (JSONObject)null);
   }

   private static Repository createDBRep(String name, String url, String user, String pass) throws Exception {
      return createDBRep(name, url, user, pass, name, name, name + "数据库资源库");
   }

   public static Repository createDBRep(String name, String type, String access, String host, String db, String port, String user, String pass, JSONObject params) throws KettleException {
      return createDBRep(name, type, access, host, db, port, user, pass, name, name, name + "数据库资源库", params);
   }

   private static Repository createDBRep(String name, String url, String user, String pass, String id, String repName, String description) throws Exception {
      initEnv();
      DatabaseMeta dataMeta = createDatabaseMeta(name, url, user, pass, true, (Repository)null);
      return createDBRep(dataMeta, id, repName, description);
   }

   public static Repository createDBRep(String name, String type, String access, String host, String db, String port, String user, String pass, String id, String repName, String description, JSONObject params) throws KettleException {
      initEnv();
      DatabaseMeta dataMeta = createDatabaseMeta(name, type, access, host, db, port, user, pass, params, true, (Repository)null);
      return createDBRep(dataMeta, id, repName, description);
   }

   public static Repository createDBRep(DatabaseMeta dataMeta, String id, String repName, String description) throws KettleException {
      KettleDatabaseRepositoryMeta kettleDatabaseMeta = new KettleDatabaseRepositoryMeta(id, repName, description, dataMeta);
      return createRep(kettleDatabaseMeta, id, repName, description);
   }

   public static DatabaseMeta createDatabaseMeta(String name, String url, String user, String pass, boolean replace, Repository repository) throws Exception {
      JSONObject urlObj = JdbcUtil.parseJdbcUrl(url);
      return "oracle".equals(urlObj.getString("dbType"))?createDatabaseMeta(name, dbTypeToKettle(urlObj.getString("dbType")), DatabaseMeta.dbAccessTypeCode[0], urlObj.getString("hostname"), urlObj.getString("databaseName"), urlObj.getString("port"), user, pass, (JSONObject)null, replace, repository):("mysql".equals(urlObj.getString("dbType"))?createDatabaseMeta(name, dbTypeToKettle(urlObj.getString("dbType")), DatabaseMeta.dbAccessTypeCode[0], urlObj.getString("hostname"), urlObj.getString("databaseName"), urlObj.getString("port"), user, pass, urlObj.getJSONObject("paramObj"), replace, repository):null);
   }

   public static DatabaseMeta createDatabaseMeta(String name, String type, String access, String host, String db, String port, String user, String pass, JSONObject params, boolean replace, Repository repository) {
      DatabaseMeta dm = null;
      if(repository != null) {
         try {
            ObjectId e = repository.getDatabaseID(name);
            if(e != null && !replace) {
               dm = repository.loadDatabaseMeta(e, (String)null);
            }
         } catch (KettleException var15) {
            log.error("创建数据库元数据失败", var15);
         }
      }

      if(dm == null) {
         dm = new DatabaseMeta(name, type, access, host, db, port, user, pass);
         if(params != null) {
            Iterator e1 = params.entrySet().iterator();

            while(e1.hasNext()) {
               Entry ent = (Entry)e1.next();
               dm.addExtraOption(type, (String)ent.getKey(), ent.getValue() + "");
            }
         }

         dm.setForcingIdentifiersToLowerCase(true);
         if(repository != null) {
            try {
               repository.save(dm, (String)null, (ProgressMonitorListener)null, true);
            } catch (KettleException var14) {
               log.error("保存数据库元数据失败", var14);
            }
         }
      }

      return dm;
   }

   public static void initEnv() throws KettleException {
      if(System.getenv("KETTLE_HOME") != null) {
         System.setProperty("DI_HOME", System.getenv("KETTLE_HOME"));
         System.setProperty("KETTLE_HOME", System.getenv("KETTLE_HOME"));
         System.setProperty("org.osjava.sj.root", System.getenv("KETTLE_HOME") + "/simple-jndi");
         log.info("KETTLE_HOME配置[能自动加载该目录下plugins中的插件]：" + System.getenv("KETTLE_HOME"));
      }

      if(System.getenv("KETTLE_JNDI_ROOT") != null) {
         System.setProperty("org.osjava.sj.root", System.getenv("KETTLE_JNDI_ROOT"));
         log.info("Simple-jndi配置根路径：" + System.getenv("KETTLE_JNDI_ROOT"));
      }

      if(!KettleEnvironment.isInitialized()) {
         KettleEnvironment.init();
         KettleClientEnvironment.getInstance().setClient(ClientType.SPOON);
      }

   }

   public static Repository createRep(BaseRepositoryMeta baseRepositoryMeta, String id, String repName, String description) throws KettleException {
      if(use(id) != null) {
         if(repository.getName().equals(use(id).getName())) {
            repository = null;
         }

         use(id).disconnect();
      }

      Object repository = null;
      if(baseRepositoryMeta instanceof KettleDatabaseRepositoryMeta) {
         repository = new KettleDatabaseRepository();
         ((Repository)repository).init((KettleDatabaseRepositoryMeta)baseRepositoryMeta);
      } else {
         repository = new KettleFileRepository();
         ((Repository)repository).init((KettleFileRepositoryMeta)baseRepositoryMeta);
      }

      if(repository == null) {
         repository = (Repository)repository;
      }

      repMap.put(id, repository);
      log.info(((Repository)repository).getName() + "资源库初始化成功");
      return (Repository)repository;
   }

   public static Repository connect() throws KettleSecurityException, KettleException {
      return connect((String)null, (String)null);
   }

   public static Repository connect(String username, String password) throws KettleSecurityException, KettleException {
      repository.connect(username, password);
      log.info(repository.getName() + "资源库连接成功");
      return repository;
   }

   public static void setRepository(Repository repository) {
      repository = repository;
   }

   public static void destroy() {
      if(repository != null) {
         repository.disconnect();
         log.info(repository.getName() + "资源库释放成功");
         repository = null;
      }

   }

   public static JobMeta loadJob(long jobId) throws KettleException {
      return repository.loadJob(new LongObjectId(jobId), (String)null);
   }

   public static JobMeta loadJob(String jobId) throws KettleException {
      return repository.loadJob(new StringObjectId(jobId), (String)null);
   }

   public static JobMeta loadJob(String jobname, String directory) throws KettleException {
      return loadJob(jobname, directory, repository);
   }

   public static JobMeta loadJob(String jobname, String directory, Repository repository) throws KettleException {
      RepositoryDirectoryInterface dir = repository.findDirectory(directory);
      return repository.loadJob(jobname, dir, (ProgressMonitorListener)null, (String)null);
   }

   public static JobMeta loadJob(String jobname, long directory) throws KettleException {
      return loadJob(jobname, directory, repository);
   }

   public static JobMeta loadJob(String jobname, long directory, Repository repository) throws KettleException {
      RepositoryDirectoryInterface dir = repository.findDirectory(new LongObjectId(directory));
      return repository.loadJob(jobname, dir, (ProgressMonitorListener)null, (String)null);
   }

   public static void delJob(long id_job) throws KettleException {
      delJob(id_job, repository);
   }

   public static void delJob(long id_job, Repository repository) throws KettleException {
      repository.deleteJob(new LongObjectId(id_job));
   }

   public static TransMeta loadTrans(String transname, String directory) throws KettleException {
      return loadTrans(transname, directory, repository);
   }

   public static TransMeta loadTrans(String transname, String directory, Repository repository) throws KettleException {
      RepositoryDirectoryInterface dir = repository.findDirectory(directory);
      return repository.loadTransformation(transname, dir, (ProgressMonitorListener)null, true, (String)null);
   }

   public static TransMeta loadTrans(JobMeta jobMeta, String teansName) throws KettleException {
      JobEntryTrans trans = (JobEntryTrans)((JobEntryTrans)jobMeta.findJobEntry(teansName).getEntry());
      TransMeta transMeta = loadTrans(trans.getTransname(), trans.getDirectory());
      return transMeta;
   }

   public static JobEntryBase loadJobEntry(JobMeta jobMeta, String jobEntryName, JobEntryBase jobEntryMeta) throws KettleException {
      return loadJobEntry(jobMeta.findJobEntry(jobEntryName).getEntry().getObjectId(), jobEntryMeta);
   }

   public static JobEntryBase loadJobEntry(ObjectId entryId, JobEntryBase jobEntryMeta) throws KettleException {
      jobEntryMeta.loadRep(repository, (IMetaStore)null, entryId, (List)null, (List)null);
      jobEntryMeta.setObjectId(entryId);
      return jobEntryMeta;
   }

   public static JobEntrySpecial findStart(JobMeta jobMeta) {
      for(int i = 0; i < jobMeta.nrJobEntries(); ++i) {
         JobEntryCopy jec = jobMeta.getJobEntry(i);
         JobEntryInterface je = jec.getEntry();
         if(je.getPluginId().equals("SPECIAL")) {
            return (JobEntrySpecial)je;
         }
      }

      return null;
   }

   public static void saveRepositoryElement(RepositoryElementInterface repositoryElement) throws KettleException {
      saveRepositoryElement(getInstanceRep(), repositoryElement);
   }

   public static void saveRepositoryElement(Repository repository, RepositoryElementInterface repositoryElement) throws KettleException {
      repository.save(repositoryElement, (String)null, (ProgressMonitorListener)null, true);
   }

   public static void saveTrans(TransMeta transMeta) throws KettleException {
      saveRepositoryElement(repository, transMeta);
   }

   public static void saveTrans(Repository repository, TransMeta transMeta) throws KettleException {
      saveRepositoryElement(repository, transMeta);
   }

   public static void saveJob(JobMeta jobMeta) throws KettleException {
      saveRepositoryElement(repository, jobMeta);
   }

   public static void saveJob(Repository repository, JobMeta jobMeta) throws KettleException {
      saveRepositoryElement(repository, jobMeta);
   }

   public static boolean isDirectoryExist(Repository repository, String directoryName) {
      try {
         RepositoryDirectoryInterface e = repository.findDirectory(directoryName);
         return e != null;
      } catch (KettleException var3) {
         log.error("判断job目录是否存在失败！", var3);
         return false;
      }
   }

   public static RepositoryDirectoryInterface getOrMakeDirectory(String parentDirectory, String directoryName) throws KettleException {
      RepositoryDirectoryInterface dir = repository.findDirectory(parentDirectory + "/" + directoryName);
      return dir == null?repository.createRepositoryDirectory(repository.findDirectory(parentDirectory), directoryName):dir;
   }

   public static RepositoryDirectoryInterface makeDirs(String directoryName) throws KettleException {
      if(!StringUtil.isNotBlank(directoryName)) {
         return null;
      } else {
         String parentDirectory = "/";
         String[] dirArr = directoryName.replace("\\", "/").replace("//", "/").split("/");
         String[] arr$ = dirArr;
         int len$ = dirArr.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String dirStr = arr$[i$];
            parentDirectory = getOrMakeDirectory(parentDirectory, dirStr).getPath();
         }

         return getOrMakeDirectory(parentDirectory, (String)null);
      }
   }

   public static String getDirectory(long dirId) throws KettleException {
      return getDirectory(new LongObjectId(dirId));
   }

   public static String getDirectory(ObjectId dirId) throws KettleException {
      RepositoryDirectoryInterface dir = repository.findDirectory(dirId);
      return dir == null?null:dir.getPath();
   }

   public static void setStepToTrans(TransMeta teans, String stepName, StepMetaInterface smi) {
      StepMeta step = teans.findStep(stepName);
      step.setStepMetaInterface(smi);
   }

   public static void setStepToTransAndSave(TransMeta teans, String stepName, StepMetaInterface smi) throws KettleException {
      setStepToTrans(teans, stepName, smi);
      saveTrans(teans);
   }

   public static List stepPreview(TransMeta teans, String testStep, StepMetaInterface smi, int previewSize) {
      TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(teans, smi, testStep);
      TransPreviewUtil tpu = new TransPreviewUtil(previewMeta, new String[]{testStep}, new int[]{previewSize});
      tpu.doPreview();
      return TransPreviewUtil.getData(tpu.getPreviewRowsMeta(testStep), tpu.getPreviewRows(testStep));
   }

   public static void jobCopy(String jobName, String jobPath, Repository fromRepository, Repository toRepository) throws KettleException {
      JobMeta jobMeta = loadJob(jobName, jobPath, fromRepository);
      Iterator i$ = jobMeta.getJobCopies().iterator();

      while(i$.hasNext()) {
         JobEntryCopy jec = (JobEntryCopy)i$.next();
         if(jec.isTransformation()) {
            JobEntryTrans jej = (JobEntryTrans)jec.getEntry();
            transCopy(jej.getObjectName(), jej.getDirectory(), fromRepository, toRepository);
         } else if(jec.isJob()) {
            JobEntryJob jej1 = (JobEntryJob)jec.getEntry();
            jobCopy(jej1.getObjectName(), jej1.getDirectory(), fromRepository, toRepository);
         }
      }

      jobMeta.setRepository(toRepository);
      jobMeta.setMetaStore(toRepository.getMetaStore());
      if(!isDirectoryExist(toRepository, jobPath)) {
         toRepository.createRepositoryDirectory(toRepository.findDirectory("/"), jobPath);
      }

      saveJob(toRepository, jobMeta);
   }

   public static void transCopy(String transName, String transPath, Repository fromRepository, Repository toRepository) throws KettleException {
      TransMeta tm = loadTrans(transName, transPath, fromRepository);
      Iterator i$ = tm.getSteps().iterator();

      while(i$.hasNext()) {
         StepMeta sm = (StepMeta)i$.next();
         if(sm.isJobExecutor()) {
            JobExecutorMeta te = (JobExecutorMeta)sm.getStepMetaInterface();
            jobCopy(te.getJobName(), te.getDirectoryPath(), fromRepository, toRepository);
         } else if(sm.getStepMetaInterface() instanceof TransExecutorMeta) {
            TransExecutorMeta te1 = (TransExecutorMeta)sm.getStepMetaInterface();
            transCopy(te1.getTransName(), te1.getDirectoryPath(), fromRepository, toRepository);
         }
      }

      if(!isDirectoryExist(toRepository, transPath)) {
         toRepository.createRepositoryDirectory(toRepository.findDirectory("/"), transPath);
      }

      tm.setRepository(toRepository);
      tm.setMetaStore(toRepository.getMetaStore());
      saveTrans(toRepository, tm);
   }

   public static ObjectId getJobId(JobMeta jm) {
      return getJobId(jm.getName(), jm.getRepositoryDirectory());
   }

   public static ObjectId getJobId(String name, RepositoryDirectoryInterface repositoryDirectory) {
      try {
         return repository.getJobId(name, repositoryDirectory);
      } catch (KettleException var3) {
         log.debug("获取作业id失败", var3);
         return null;
      }
   }

   public static ObjectId getTransformationID(TransMeta tm) {
      return getTransformationID(tm.getName(), tm.getRepositoryDirectory());
   }

   public static ObjectId getTransformationID(String name, RepositoryDirectoryInterface repositoryDirectory) {
      try {
         return repository.getTransformationID(name, repositoryDirectory);
      } catch (KettleException var3) {
         log.debug("获取转换id失败", var3);
         return null;
      }
   }

   public static void repairTransHop(TransMeta tm) {
      for(int i = 0; i < tm.nrTransHops(); ++i) {
         TransHopMeta hop = tm.getTransHop(i);
         hop.setFromStep(tm.findStep(hop.getFromStep().getName()));
         hop.setToStep(tm.findStep(hop.getToStep().getName()));
      }

   }

   public static void setParams(NamedParams target, NamedParams source, Map params) {
      target.eraseParameters();

      try {
         String[] e = source.listParameters();
         int len$ = e.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String key = e[i$];
            String defaultVal = source.getParameterDefault(key);
            if(params.containsKey(key)) {
               defaultVal = (String)params.get(key);
            }

            target.addParameterDefinition(key, defaultVal, source.getParameterDescription(key));
         }
      } catch (Exception var8) {
         log.error("保存JOB失败", var8);
      }

   }

   public static void repairHop(JobMeta jm) {
      Iterator i$ = jm.getJobhops().iterator();

      while(i$.hasNext()) {
         JobHopMeta hop = (JobHopMeta)i$.next();
         hop.setFromEntry(jm.findJobEntry(hop.getFromEntry().getName()));
         hop.setToEntry(jm.findJobEntry(hop.getToEntry().getName()));
      }

   }

   public static TransMeta getTransMetaTemplate() {
      return transMetaTemplate;
   }

   public static void setTransMetaTemplate(TransMeta transMetaTemplate) {
      transMetaTemplate = transMetaTemplate;
   }

   public static JobMeta getJobMetaTemplate() {
      return jobMetaTemplate;
   }

   public static void setJobMetaTemplate(JobMeta jobMetaTemplate) {
      jobMetaTemplate = jobMetaTemplate;
   }

   public static String getProp(VariableSpace vs, String key) {
      String value = vs.environmentSubstitute("${" + key + "}");
      return value.startsWith("${")?"":value;
   }

   public static JSONObject getPropJSONObject(VariableSpace vs, String key) {
      String value = getProp(vs, key);
      return StringUtil.isNotBlank(value)?JSON.parseObject(value):null;
   }

   public static Job getRootJob(Job rootjob) {
      while(rootjob != null && rootjob.getParentJob() != null) {
         rootjob = rootjob.getParentJob();
      }

      return rootjob;
   }

   public static Job getRootJob(JobEntryBase jee) {
      Job rootjob = jee.getParentJob();
      return getRootJob(rootjob);
   }

   public static Job getRootJob(StepInterface si) {
      Job rootjob = si.getTrans().getParentJob();
      return getRootJob(rootjob);
   }

   public static String getRootJobId(JobEntryBase jee) {
      return getRootJob(jee).getObjectId().getId();
   }

   public static String getRootJobId(StepInterface si) {
      Job rootjob = getRootJob(si);
      return rootjob != null?rootjob.getObjectId().getId():null;
   }

   public static String getRootJobName(StepInterface si) {
      Job rootjob = getRootJob(si);
      return rootjob != null?rootjob.getObjectName():null;
   }

}
