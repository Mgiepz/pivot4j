/*
 * ====================================================================
 * This software is subject to the terms of the Common Public License
 * Agreement, available at the following URL:
 *   http://www.opensource.org/licenses/cpl.html .
 * You must accept the terms of that agreement to use this software.
 * ====================================================================
 */
package com.eyeq.pivot4j.ui.property;

import com.eyeq.pivot4j.state.Bookmarkable;
import com.eyeq.pivot4j.state.Configurable;

public interface PropertySource extends Configurable, Bookmarkable {

	/**
	 * @param property
	 */
	void setProperty(Property property);

	/**
	 * @param name
	 */
	Property getProperty(String name);

	/**
	 * @param name
	 */
	void removeProperty(String name);

	/**
	 * @param name
	 */
	boolean hasProperty(String name);

	void clearProperties();
}
