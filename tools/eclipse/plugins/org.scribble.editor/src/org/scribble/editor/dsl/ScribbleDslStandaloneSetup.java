/*
* generated by Xtext
*/
package org.scribble.editor.dsl;

/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class ScribbleDslStandaloneSetup extends ScribbleDslStandaloneSetupGenerated{

	public static void doSetup() {
		new ScribbleDslStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}
