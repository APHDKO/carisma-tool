/*******************************************************************************
 * Copyright (c) 2011 Software Engineering Institute, TU Dortmund.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    {SecSE group} - initial API and implementation and/or initial documentation
 *******************************************************************************/
package carisma.check.umlchange.efficiency;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.resource.Resource;

import carisma.core.analysis.AnalysisHost;
import carisma.core.analysis.RegisterInUseException;
import carisma.core.analysis.RegisterNotInUseException;
import carisma.core.analysis.result.AnalysisResultMessage;
import carisma.evolution.Delta;
import carisma.evolution.DeltaList;
import carisma.evolution.uml2.ModifierMap;
import carisma.evolution.uml2.UMLModifier;

/**
 * Implementation of the {@link AnalysisHost} for testing purpose.
 * @author Klaus Rudack
 *
 */
public class TestingHost implements AnalysisHost {
	
	
	/**
	 * boolean whether to print messages to the console or not.
	 */
	private boolean toSystemHost = false;
	
	/**
	 * the resource that will be analyzed.
	 */
	private Resource res = null;
	
	/**
	 * register to store comprehensive check informations.
	 */
	private Map<String, Object> register = new HashMap<String, Object>();
	
	/**
	 * key for the modifier entry in the register.
	 */
	private String mODIFIERS_REGISTER_KEY = "carisma.data.evolution.modifiers";
	
	/**
	 * key for the delta entry in the register.
	 */
	private String deltaRegistry = "carisma.data.evolution.deltas";
	
	/**
	 * constructor.
	 * @param printToSystemOut true if messages should be printed on the console, false otherwise
	 */
	public TestingHost(final boolean printToSystemOut) {
		toSystemHost = printToSystemOut;
	}
	
	@Override
	public final void addResultMessage(final AnalysisResultMessage resultMessage) {
		if (toSystemHost) {
			System.out.println(resultMessage.getText());
		}
	}

	@Override
	public final void appendToReport(final String text) {
		if (toSystemHost) {
			System.out.print(text);
		}
	}

	@Override
	public final void appendLineToReport(final String text) {
		if (toSystemHost) {
			System.out.println(text);
		}
	}

	@Override
	public final Resource getAnalyzedModel() {
		return res;
	}
	
	/**
	 * sets the resource.
	 * @param resource the new resource
	 */
	public final void setAnalyzedModel(final Resource resource) {
		res = resource;
	}
	
	@Override
	public final String getCurrentModelFilename() {
		return res.getURI().toString();
	}

	@Override
	public final void putToRegister(final String registerName, final Object data) throws RegisterInUseException {
		if (isRegisterInUse(registerName)) {
			throw new RegisterInUseException(registerName);
		}
		register.put(registerName, data);
	}

	@Override
	public final boolean isRegisterInUse(final String registerName) {
		if (register.keySet().contains(registerName)) {
			return true;
		}
		return false;
	}

	@Override
	public final Object getFromRegister(final String registerName) throws RegisterNotInUseException {
		if (!register.keySet().contains(registerName)) {
			throw new RegisterNotInUseException(registerName);
		}
		return register.get(registerName);
	}

	@Override
	public final Object removeFromRegister(final String registerName) throws RegisterNotInUseException {
		if (!register.keySet().contains(registerName)) {
			throw new RegisterNotInUseException(registerName);
		}
		return register.remove(registerName);
	}

	@Override
	public final void displayError(final String message) {
		System.err.println("Error: " + message);
	}

	@Override
	public final File getFileToBeWritten(final File file) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * deletes the content of the TestingHost.
	 */
	public final void delete() {
		deleteModifier();
		res = null;
		register = null;
	}
	
	/**
	 * deletes the modifiers an loaded models.
	 */
	private void deleteModifier() {
		if (register.keySet().contains(mODIFIERS_REGISTER_KEY)) {
			ModifierMap deltaModifiers = (ModifierMap) register.get(mODIFIERS_REGISTER_KEY);
			DeltaList deltas = (DeltaList) register.get(deltaRegistry);
			try {
			if (deltaModifiers != null) {
				for (Delta d : deltas.getAllDeltas()) {
					UMLModifier umlMod = deltaModifiers.get(d);
						if (umlMod.getModifiedModel() != null) {
							umlMod.getModifiedModel().destroy();
						}
						if (umlMod.getOriginalModel() != null) {
							umlMod.getOriginalModel().destroy();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
