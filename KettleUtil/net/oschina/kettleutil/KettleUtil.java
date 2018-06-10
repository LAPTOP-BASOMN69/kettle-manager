package net.oschina.kettleutil;

import net.oschina.kettleutil.KettleUtilData;
import net.oschina.kettleutil.KettleUtilMeta;
import net.oschina.kettleutil.KettleUtilRunBase;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class KettleUtil extends BaseStep implements StepInterface {

   private KettleUtilData data;
   private KettleUtilMeta meta;
   private KettleUtilRunBase kui;


   public KettleUtil(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
      super(s, stepDataInterface, c, t, dis);
   }

   public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
      this.meta = (KettleUtilMeta)smi;
      this.data = (KettleUtilData)sdi;
      if(StringUtils.isNotBlank(this.meta.getClassName())) {
         try {
            if(this.first) {
               this.kui = (KettleUtilRunBase)Class.forName(this.environmentSubstitute(this.meta.getClassName())).newInstance();
               this.kui.setKu(this);
               this.kui.setMeta(this.meta, this);
            }

            this.kui.setData(this.data);
            return this.kui.run();
         } catch (Exception var4) {
            this.setErrors(this.getErrors() + 1L);
            this.logError("运行失败," + this.meta.getClassName() + "," + this.environmentSubstitute(this.meta.getConfigInfo()), var4);
            return this.defaultRun();
         }
      } else {
         return this.defaultRun();
      }
   }

   public boolean defaultRun() throws KettleException, KettleStepException {
      Object[] r = this.getRow();
      if(r == null) {
         this.setOutputDone();
         return false;
      } else {
         if(this.first) {
            this.first = false;
            this.data.outputRowMeta = this.getInputRowMeta().clone();
            this.meta.getFields(this.data.outputRowMeta, this.getStepname(), (RowMetaInterface[])null, (StepMeta)null, this);
            this.logBasic("template step initialized successfully");
         }

         Object[] outputRow = RowDataUtil.createResizedCopy(r, this.data.outputRowMeta.size());
         this.putRow(this.data.outputRowMeta, outputRow);
         if(this.checkFeedback(this.getLinesRead())) {
            this.logBasic("Linenr " + this.getLinesRead());
         }

         return true;
      }
   }

   public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
      this.meta = (KettleUtilMeta)smi;
      this.data = (KettleUtilData)sdi;
      return super.init(smi, sdi);
   }

   public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
      this.meta = (KettleUtilMeta)smi;
      this.data = (KettleUtilData)sdi;
      super.dispose(smi, sdi);
   }
}
