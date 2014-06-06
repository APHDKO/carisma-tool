/*******************************************************************************
 * Copyright (c) 2012 Software Engineering Institute, TU Dortmund.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    {SecSE group} - initial API and implementation and/or initial documentation
 *******************************************************************************/
package carisma.regulatory.ruleallocator;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * Configures the Workbench for the execution of the Application.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	/**
	 * The perspective id.
	 */
	private static final String PERSPECTIVE_ID = "carisma.regulatory.ruleallocator.perspective";

	@Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
    		final IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
}
