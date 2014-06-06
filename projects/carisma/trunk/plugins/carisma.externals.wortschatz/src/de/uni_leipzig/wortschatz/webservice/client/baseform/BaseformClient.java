/*
 * BaseformBaseformClient.java
 *
 * Created on 20. April 2004, 08:50
 */

package de.uni_leipzig.wortschatz.webservice.client.baseform;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.rpc.ServiceException;

import org.apache.axis.attachments.AttachmentPart;

import de.uni_leipzig.wortschatz.webservice.TestInterface;
import de.uni_leipzig.wortschatz.webservice.client.utilities.DataMatrix;
import de.uni_leipzig.wortschatz.webservice.client.utilities.DataVector;
import de.uni_leipzig.wortschatz.webservice.client.utilities.RequestParameter;
import de.uni_leipzig.wortschatz.webservice.client.utilities.ResponseParameter;

/**
 * 
 * @author Administrator
 */
public class BaseformClient implements TestInterface {

	private BaseformSoapBindingStub binding = null;
	private RequestParameter objRequestParam = new RequestParameter();
	private ResponseParameter objResponseParam = null;

	private final HashMap objParams = new HashMap();

	private Vector objAttachments = null;
	private String strUsername = null;
	private String strPasswd = null;

	private String strBaseformExecutionTime = null;
	private String[][] strResult = null;

	boolean isEmptyResult = false;

	/** Creates a new instance of BaseformTestBaseform. */
	public BaseformClient() throws Exception {

		objRequestParam.setCorpus("de");

		try {
			binding = (BaseformSoapBindingStub) new BaseformServiceLocator().getBaseform();
		} catch (ServiceException jre) {
			throw new Exception(jre.getMessage());
		}

		objAttachments = new Vector();
	}

	private void init() {

	}

	public final ResponseParameter execute() throws Exception {

		isEmptyResult = false;

		binding.setTimeout(60000);
		binding.setUsername(strUsername);
		binding.setPassword(strPasswd);
		binding.clearAttachments();

		Iterator objIterator = objAttachments.iterator();
		while (objIterator.hasNext()) {

			Object obj = objIterator.next();
			binding.addAttachment(obj);
		}

		DataMatrix objMatrix = new DataMatrix();
		DataVector[] objDataRows = new DataVector[objParams.size()];

		if (!objParams.isEmpty()) {
			java.util.Set objParamKeysSet = objParams.keySet();
			java.util.Iterator objParamKeysIter = objParamKeysSet.iterator();

			int paramIndex = 0;
			while (objParamKeysIter.hasNext()) {
				DataVector objDataRow = new DataVector();
				String[] strProperties = new String[2];

				strProperties[0] = (String) objParamKeysIter.next();
				strProperties[1] = (String) objParams.get(strProperties[0]);

				objDataRow.setDataRow(strProperties);
				objDataRows[paramIndex] = objDataRow;
				paramIndex++;
			}
		}

		objMatrix.setDataVectors(objDataRows);
		objRequestParam.setParameters(objMatrix);

		long longStartTime = System.currentTimeMillis();

		try {
			objResponseParam = binding.execute(objRequestParam);
		} catch (Exception rme) {
			throw new Exception(rme.getMessage());
		}

		long longEndTime = System.currentTimeMillis();

		Double objTime = new Double(((double) (longEndTime - longStartTime)) / 1000);

		strBaseformExecutionTime = objTime.toString() + " s";

		DataMatrix objResult = objResponseParam.getResult();

		if ((objResult.getDataVectors() == null) || (objResult.getDataVectors().length == 0)) {
			isEmptyResult = true;
			strResult = new String[0][0];
		} else {
			isEmptyResult = false;

			int intRowSize = objResult.getDataVectors().length;
			int intColumnSize = objResult.getDataVectors()[0].getDataRow().length;

			strResult = new String[intRowSize][intColumnSize];

			for (int i = 0; i < objResult.getDataVectors().length; i++) {
				DataVector objRow = objResult.getDataVectors(i);

				for (int j = 0; j < objRow.getDataRow().length; j++) {
					strResult[i][j] = objRow.getDataRow(j);
				}
			}
		}

		objParams.clear();

		return objResponseParam;
	}

	@Override
	public final String ping() throws Exception {

		try {
			binding = (BaseformSoapBindingStub) new BaseformServiceLocator().getBaseform();
		} catch (ServiceException jre) {
			throw new Exception(jre.getMessage());
		}

		binding.setTimeout(60000);

		String value = null;

		try {

			value = binding.ping();

		} catch (RemoteException rme) {
			throw new Exception(rme.getMessage());
		}
		return value;
	}

	public final String getServiceName() {
		return "Baseform Client";
	}

	public final String getDescription() {
		return "Returns the lemmatized (base) form of the input word.";
	}

	public final String getWebServiceType() {
		return "de.uni_leipzig.wortschatz.webservice.webservicetypes.MySQLSelectType";
	}

	public final String[] getDBFields() {
		return new String[] { "v.wort_bin", "wg.wortart" };
	}

	public final String[] getInputFields() {
		return new String[] { "Wort" };
	}

	public final String[] getCorpus() {
		return new String[] { "de", "en", "webservice", "es", "fr", "fr05", "fr05_100K", "fr05_1M", "fr05_300K", "fr05_3M", "it", "it100K", "it300K", "it1M", "it3M", "nl", "nl100K", "nl300K", "nl1M", "de07", "ngrams" };
	}

	@Override
	public final void setUsername(final String strUsername) {
		this.strUsername = strUsername;
	}

	@Override
	public final void setPassword(final String strPassword) {
		this.strPasswd = strPassword;
	}

	public final void setRequestParameter(final RequestParameter objRequestParameter) {
		this.objRequestParam = objRequestParameter;
	}

	public final String getServiceAuthorizationLevel() {
		return "FREE";
	}

	public final void addAttachment(final String strInputField, final String strFileName) throws Exception {

		AttachmentPart objAttachmentPart = null;

		if (new File(strFileName).exists()) {
			javax.activation.DataSource ds = new javax.activation.FileDataSource(strFileName);
			javax.activation.DataHandler dh = new javax.activation.DataHandler(ds);
			objAttachmentPart = new AttachmentPart(dh);
			objAttachmentPart.setMimeHeader("Ordinal", "" + objAttachments.size() + 1);
			objAttachmentPart.setContentLocation(ds.getName());
			objAttachmentPart.setMimeHeader("InputField-Name", strInputField);
			objAttachments.add(objAttachmentPart);
		} else {
			throw new Exception("File " + strFileName + " not found.");
		}
	}

	public final void addAttachment(final java.util.HashMap objAttachmentsMap) throws Exception {
		java.util.Set objSet = objAttachmentsMap.keySet();
		java.util.Iterator objIterator = objSet.iterator();

		objAttachments.removeAllElements();

		while (objIterator.hasNext()) {
			String strInputField = (String) objIterator.next();

			try {
				addAttachment(strInputField, (String) objAttachmentsMap.get(strInputField));
			} catch (Exception e) {
				throw new Exception(e.getMessage());
			}
		}
	}

	public final void setCorpus(final String strCorpus) {
		objRequestParam.setCorpus(strCorpus);
	}

	public final void addParameter(final String strInputField, final String strValue) {
		objParams.put(strInputField, strValue);
	}

	public final String getServerExecutionTime() {
		return objResponseParam.getExecutionTime();
	}

	public final int getUserAmount() {
		return objResponseParam.getUserAmount();
	}

	public final int getUserMaxLimit() {
		return objResponseParam.getUserMaxLimit();
	}

	public final int getServiceMagnitude() {
		return objResponseParam.getServiceMagnitude();
	}

	public final String getBaseformExecutionTime() {
		return strBaseformExecutionTime;
	}

	public final boolean isEmptyResult() {
		return isEmptyResult;
	}

	public final String[][] getResult() {
		return strResult;
	}

	public final Object[] getAttachments() {
		return binding.getAttachments();
	}

	public static int main(final String[] args) {
		try {
			BaseformClient objBaseform = new BaseformClient();

			objBaseform.setPassword(System.getProperty("Password"));
			objBaseform.setUsername(System.getProperty("UserName"));

			for (int i = 0; i < args.length; i++) {

				String[] strParameter = args[i].split("=");

				if (strParameter[0].toLowerCase().startsWith("corpus")) {
					objBaseform.setCorpus(strParameter[1]);
				} else {
					objBaseform.addParameter(strParameter[0], strParameter[1]);
				}
			}

			ResponseParameter objRespParam = null;

			objRespParam = objBaseform.execute();

			if (objBaseform.isEmptyResult()) {
				System.out.println("empty result");
			} else {
				String[][] strResult = objBaseform.getResult();

				for (int i = 0; i < strResult.length; i++) {

					for (int j = 0; j < strResult[0].length; j++) {
						System.out.println(strResult[i][j]);
					}

					System.out.println("");
				}
			}

			System.out.println("\n");
			System.out.println("client execution time : " + objBaseform.getBaseformExecutionTime());
			System.out.println("server execution time : " + objBaseform.getServerExecutionTime());
			System.out.println("\n");
			System.out.println("user amount           :  " + objBaseform.getUserAmount());
			System.out.println("max limit             : " + objBaseform.getUserMaxLimit());
			System.out.println("service's magnitude   :  " + objBaseform.getServiceMagnitude());
			System.out.println("\n\n\n");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return 0;
	}
}
