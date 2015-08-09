package edu.cmu.emfta.actions;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

import edu.cmu.emfta.preferences.PreferenceInitializer;

/** 
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "Emfta";
	public static final String EMFTA_MARKER = "EmftaMarker";

	// The shared instance
	private static Activator plugin;

	private IPreferenceStore preferenceStore = new ScopedPreferenceStore(ConfigurationScope.INSTANCE,
			Activator.PLUGIN_ID);

	/**
	 * The constructor
	 */
	public Activator() {
		System.out.println("starts plugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Call the preference initializer manually because eclipse will not call it if the preference page uses a
// scoped preference store
		new PreferenceInitializer().initializeDefaultPreferences();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return preferenceStore;
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance 
	 */
	public static Activator getDefault() {
		return plugin;
	}

}