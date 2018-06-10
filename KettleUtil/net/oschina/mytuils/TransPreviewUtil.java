package net.oschina.mytuils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.debug.BreakPointListener;
import org.pentaho.di.trans.debug.StepDebugMeta;
import org.pentaho.di.trans.debug.TransDebugMeta;
import org.pentaho.di.trans.step.StepMeta;

public class TransPreviewUtil {

   public static final int MAX_BINARY_STRING_PREVIEW_SIZE = 1000000;
   private static Log log = LogFactory.getLog(TransPreviewUtil.class);
   private TransMeta transMeta;
   private String[] previewStepNames;
   private int[] previewSize;
   private Trans trans;
   private boolean cancelled;
   private String loggingText;
   private TransDebugMeta transDebugMeta;


   public TransPreviewUtil(TransMeta transMeta, String[] previewStepNames, int[] previewSize) {
      this.transMeta = transMeta;
      this.previewStepNames = previewStepNames;
      this.previewSize = previewSize;
      this.cancelled = false;
   }

   public void doPreview() {
      this.trans = new Trans(this.transMeta);

      try {
         this.trans.prepareExecution((String[])null);
      } catch (KettleException var6) {
         log.error("", var6);
         return;
      }

      this.transDebugMeta = new TransDebugMeta(this.transMeta);

      for(int previewComplete = 0; previewComplete < this.previewStepNames.length; ++previewComplete) {
         StepMeta e = this.transMeta.findStep(this.previewStepNames[previewComplete]);
         StepDebugMeta stepDebugMeta = new StepDebugMeta(e);
         stepDebugMeta.setReadingFirstRows(true);
         stepDebugMeta.setRowCount(this.previewSize[previewComplete]);
         this.transDebugMeta.getStepDebugMetaMap().put(e, stepDebugMeta);
      }

      this.transDebugMeta.addRowListenersToTransformation(this.trans);

      try {
         this.trans.startThreads();
      } catch (KettleException var5) {
         log.error("", var5);
         return;
      }

      final ArrayList var7 = new ArrayList();

      while(var7.size() < this.previewStepNames.length && !this.trans.isFinished()) {
         this.transDebugMeta.addBreakPointListers(new BreakPointListener() {
            public void breakPointHit(TransDebugMeta transDebugMeta, StepDebugMeta stepDebugMeta, RowMetaInterface rowBufferMeta, List rowBuffer) {
               String stepName = stepDebugMeta.getStepMeta().getName();
               var7.add(stepName);
            }
         });

         try {
            Thread.sleep(500L);
         } catch (InterruptedException var4) {
            log.error("", var4);
         }
      }

      this.trans.stopAll();
      this.loggingText = KettleLogStore.getAppender().getBuffer(this.trans.getLogChannel().getLogChannelId(), true).toString();
   }

   public List getPreviewRows(String stepname) {
      if(this.transDebugMeta == null) {
         return null;
      } else {
         Iterator i$ = this.transDebugMeta.getStepDebugMetaMap().keySet().iterator();

         StepMeta stepMeta;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            stepMeta = (StepMeta)i$.next();
         } while(!stepMeta.getName().equals(stepname));

         StepDebugMeta stepDebugMeta = (StepDebugMeta)this.transDebugMeta.getStepDebugMetaMap().get(stepMeta);
         return stepDebugMeta.getRowBuffer();
      }
   }

   public RowMetaInterface getPreviewRowsMeta(String stepname) {
      if(this.transDebugMeta == null) {
         return null;
      } else {
         Iterator i$ = this.transDebugMeta.getStepDebugMetaMap().keySet().iterator();

         StepMeta stepMeta;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            stepMeta = (StepMeta)i$.next();
         } while(!stepMeta.getName().equals(stepname));

         StepDebugMeta stepDebugMeta = (StepDebugMeta)this.transDebugMeta.getStepDebugMetaMap().get(stepMeta);
         return stepDebugMeta.getRowBufferMeta();
      }
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public String getLoggingText() {
      return this.loggingText;
   }

   public Trans getTrans() {
      return this.trans;
   }

   public TransDebugMeta getTransDebugMeta() {
      return this.transDebugMeta;
   }

   public static List getData(RowMetaInterface rowMeta, List buffer) {
      ArrayList result = new ArrayList();
      new ArrayList();

      for(int i = 0; i < buffer.size(); ++i) {
         ArrayList row1 = new ArrayList();
         Object[] row = (Object[])buffer.get(i);
         getDataForRow(rowMeta, row1, row);
         result.add(row1);
      }

      return result;
   }

   public static int getDataForRow(RowMetaInterface rowMeta, List row1, Object[] row) {
      int nrErrors = 0;

      for(int c = 0; c < rowMeta.size(); ++c) {
         ValueMetaInterface v = rowMeta.getValueMeta(c);

         String show;
         try {
            show = v.getString(row[c]);
            if(v.isBinary() && show != null && show.length() > 1000000) {
               show = show.substring(0, 1000000);
            }
         } catch (KettleValueException var8) {
            ++nrErrors;
            if(nrErrors < 25) {
               log.error(Const.getStackTracker(var8));
            }

            show = null;
         } catch (ArrayIndexOutOfBoundsException var9) {
            ++nrErrors;
            if(nrErrors < 25) {
               log.error(Const.getStackTracker(var9));
            }

            show = null;
         }

         if(show != null) {
            row1.add(show);
         } else {
            row1.add("<null>");
         }
      }

      return nrErrors;
   }

}
