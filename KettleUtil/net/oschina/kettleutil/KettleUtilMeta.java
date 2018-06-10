package net.oschina.kettleutil;

import java.util.List;
import java.util.Map;
import net.oschina.kettleutil.KettleUtil;
import net.oschina.kettleutil.KettleUtilData;
//import net.oschina.kettleutil.KettleUtilDialog;
import net.oschina.kettleutil.KettleUtilRunBase;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.Shell;
//import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

public class KettleUtilMeta extends BaseStepMeta implements StepMetaInterface {
	private static Class<?> PKG = KettleUtilMeta.class;
	private String className = "";
	private String configInfo = "{}";

	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getConfigInfo() {
		return this.configInfo;
	}

	public void setConfigInfo(String configInfo) {
		this.configInfo = configInfo;
	}

	public String getXML() throws KettleValueException {
		String retval = "";
		retval = retval + "\t\t<classname>" + this.getClassName() + "</classname>" + Const.CR;
		retval = retval + "     <configinfo>" + this.getConfigInfo() + "</configinfo>" + Const.CR;
		return retval;
	}

	public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep,
			VariableSpace space) {
		if (StringUtils.isNotBlank(this.className)) {
			try {
				KettleUtilRunBase e = (KettleUtilRunBase) Class.forName(space.environmentSubstitute(this.className))
						.newInstance();
				e.setKu((KettleUtil) null);
				e.setMeta(this, space);
				e.getFields(r, origin, info, nextStep, space);
			} catch (Exception arg6) {
				this.logError("获取输出字段失败", arg6);
			}
		}

	}

	public String getDefaultConfigInfo(TransMeta transMeta, String stepName, VariableSpace space) throws Exception {
		if (StringUtils.isNotBlank(this.getClassName())) {
			KettleUtilRunBase kui = (KettleUtilRunBase) Class.forName(space.environmentSubstitute(this.getClassName()))
					.newInstance();
			kui.setKu((KettleUtil) null);
			kui.setMeta(this, space);
			return kui.getDefaultConfigInfo(transMeta, stepName);
		} else {
			return null;
		}
	}

	public Object clone() {
		Object retval = super.clone();
		return retval;
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleXMLException {
		try {
			this.setClassName(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "classname")));
			this.setConfigInfo(XMLHandler.getNodeValue(XMLHandler.getSubNode(stepnode, "configinfo")));
		} catch (Exception arg4) {
			throw new KettleXMLException("Template Plugin Unable to read step info from XML node", arg4);
		}
	}

	public void setDefault() {
		this.className = "net.oschina.kettleutil.utilrun.KurDemo";
		this.configInfo = "{}";
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev,
			String[] input, String[] output, RowMetaInterface info) {
		CheckResult cr;
		if (input.length > 0) {
			cr = new CheckResult(1, "Step is receiving info from other steps.", stepMeta);
			remarks.add(cr);
		} else {
			cr = new CheckResult(4, "No input received from other steps!", stepMeta);
			remarks.add(cr);
		}

	}

//	public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
//		return new KettleUtilDialog(shell, meta, transMeta, name);
//	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
			Trans disp) {
		return new KettleUtil(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	public StepDataInterface getStepData() {
		return new KettleUtilData();
	}

	public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters)
			throws KettleException {
		try {
			this.className = rep.getStepAttributeString(id_step, "classname");
			this.configInfo = rep.getStepAttributeString(id_step, "configinfo");
		} catch (Exception arg5) {
			throw new KettleException(BaseMessages.getString(PKG,
					"TemplateStep.Exception.UnexpectedErrorInReadingStepInfo", new String[0]), arg5);
		}
	}

	public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {
		try {
			rep.saveStepAttribute(id_transformation, id_step, "classname", this.className);
			rep.saveStepAttribute(id_transformation, id_step, "configinfo", this.configInfo);
		} catch (Exception arg4) {
			throw new KettleException(BaseMessages.getString(PKG,
					"TemplateStep.Exception.UnableToSaveStepInfoToRepository", new String[0]) + id_step, arg4);
		}
	}
}