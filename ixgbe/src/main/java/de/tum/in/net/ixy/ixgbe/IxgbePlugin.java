package de.tum.in.net.ixy.ixgbe;

import de.tum.in.net.ixy.generic.IxyDevice;
import de.tum.in.net.ixy.generic.IxyDriver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import java.io.FileNotFoundException;

/**
 * Exposed plugin interface.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public final class IxgbePlugin extends Plugin {

	/**
	 * Initializes the plugin wrapper.
	 *
	 * @param wrapper The plugin wrapper.
	 */
	public IxgbePlugin(@NotNull PluginWrapper wrapper) {
		super(wrapper);
		if (BuildConfig.DEBUG) log.info("The Ixgbe plugin has been instantiated");
	}

	/**
	 * The extension point according to Ixy's specification.
	 *
	 * @author Esaú García Sánchez-Torija
	 */
	@Extension
	public static final class IxgbeDriver implements IxyDriver {

		@Override
		@Contract(value = " -> new", pure = true)
		public @NotNull String getName() {
			return "ixgbe-ixy";
		}

		@Override
		@Contract(value = "null, _ -> fail; _, null -> fail; !null, !null -> new", pure = true)
		public @NotNull IxyDevice getDevice(@NotNull String device, @NotNull String driver) throws FileNotFoundException {
			return new IxgbeDevice(device, driver);
		}

	}

	//////////////////////////////////////////////// OVERRIDDEN METHODS ////////////////////////////////////////////////

	@Override
	public void start() {
		super.start();
		if (BuildConfig.DEBUG) log.info("The Ixgbe plugin has started");
	}

	@Override
	public void stop() {
		super.stop();
		if (BuildConfig.DEBUG) log.info("The Ixgbe plugin has stopped");
	}

}
