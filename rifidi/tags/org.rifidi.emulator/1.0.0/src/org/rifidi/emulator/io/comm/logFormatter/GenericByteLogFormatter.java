/*
 *  GenericByteLogFormatter.java
 *
 *  Created:	Dec 11, 2007
 *  Project:	RiFidi Emulator - A Software Simulation Tool for RFID Devices
 *  				http://www.rifidi.org
 *  				http://rifidi.sourceforge.net
 *  Copyright:	Pramari LLC and the Rifidi Project
 *  License:	Lesser GNU Public License (LGPL)
 *  				http://www.opensource.org/licenses/lgpl-license.html
 *  Author:    Kyle Neumeier - kyle@pramari.com
 */
package org.rifidi.emulator.io.comm.logFormatter;

import org.rifidi.utilities.formatting.ByteAndHexConvertingUtility;

/**
 * @author Kyle Neumeier - kyle@pramari.com
 *
 */
public class GenericByteLogFormatter implements LogFormatter {

	/* (non-Javadoc)
	 * @see org.rifidi.emulator.io.comm.logFormatter.LogFormatter#formatMessage(byte[])
	 */
	public String formatMessage(byte[] rawMessage) {
		return ByteAndHexConvertingUtility.toHexString(rawMessage) + "\n";
	}

}
