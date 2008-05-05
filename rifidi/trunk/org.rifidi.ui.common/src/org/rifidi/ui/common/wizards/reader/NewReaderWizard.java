/*
 *  NewReaderWizard.java
 *
 *  Created:	Feb 28, 2007
 *  Project:	RiFidi Emulator - A Software Simulation Tool for RFID Devices
 *  				http://www.rifidi.org
 *  				http://rifidi.sourceforge.net
 *  Copyright:	Pramari LLC and the Rifidi Project
 *  License:	Lesser GNU Public License (LGPL)
 *  				http://www.opensource.org/licenses/lgpl-license.html
 */
package org.rifidi.ui.common.wizards.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.rifidi.ui.common.reader.UIReader;
import org.rifidi.ui.common.registry.ReaderRegistry;
import org.rifidi.ui.common.wizards.reader.pages.NewReaderDynamicWizardPage;
import org.rifidi.ui.common.wizards.reader.pages.NewReaderGPIOWizardPage;
import org.rifidi.ui.common.wizards.reader.pages.NewReaderWizardSelectionPage;

/**
 * A wizard to create a new reader.
 * 
 * @author Jochen Mader - jochen@pramari.com
 * @author Andreas Huebner - andreas@pramari.com
 */
public class NewReaderWizard extends Wizard {

	private static Log logger = LogFactory.getLog(NewReaderWizard.class);

	private NewReaderWizardSelectionPage newReaderWizardSelectionPage;

	private NewReaderDynamicWizardPage newReaderDynamicWizardPage;

	private NewReaderGPIOWizardPage newReaderGPIOWizardPage;

	private UIReader reader;

	public NewReaderWizard(UIReader reader) {
		super();
		this.reader = reader;
	}

	@Override
	public void addPages() {
		newReaderWizardSelectionPage = new NewReaderWizardSelectionPage(
				"readerSelectionPage", reader, ReaderRegistry.getInstance()
						.getReaderBlueprints());
		addPage(newReaderWizardSelectionPage);

		newReaderDynamicWizardPage = new NewReaderDynamicWizardPage(
				"readerCreationPage", reader, ReaderRegistry.getInstance()
						.getReaderBlueprints());
		addPage(newReaderDynamicWizardPage);

		newReaderGPIOWizardPage = new NewReaderGPIOWizardPage("gpioPage",
				reader, ReaderRegistry.getInstance().getReaderBlueprints());
		addPage(newReaderGPIOWizardPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
		newReaderWizardSelectionPage.createControl(pageContainer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		// check if the page is ready to be processed
		if (newReaderDynamicWizardPage.isPageComplete()) {
			// try {
			// logger.debug("Reader Wizard finished. Reader '"
			// + reader.getReaderName() + "' will be created.");
			// ReaderRegistry.getInstance().create(reader);
			logger.debug("Wizard finished. Reader name " + reader.getReaderName());
			return true;
			// } catch (DuplicateReaderException dre) {
			// newReaderDynamicWizardPage.setPageComplete(false);
			// } catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

		}
		return false;
	}

	@Override
	public boolean canFinish() {
		if (!newReaderDynamicWizardPage.enableGPIO()
				&& newReaderDynamicWizardPage.isPageComplete())
			return true;
		if (newReaderDynamicWizardPage.isPageComplete()
				&& newReaderGPIOWizardPage.isPageComplete())
			return true;
		return false;
	}
}
