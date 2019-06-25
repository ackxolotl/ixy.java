package de.tum.in.net.ixy.pci;

import de.tum.in.net.ixy.generic.IxyPacketBuffer;
import de.tum.in.net.ixy.generic.IxyStats;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

/**
 * Dummy class extending {@link Device}.
 *
 * @author Esaú García Sánchez-Torija
 */
final class DummyDevice extends Device {

	DummyDevice(@NotNull String name, @NotNull String driver) throws FileNotFoundException {
		super(name, driver);
	}

	@Override
	public void allocate() { }

	@Override
	public boolean isSupported() {
		return false;
	}

	@Override
	protected int getRegister(int offset) {
		return 0;
	}

	@Override
	protected void setRegister(int offset, int value) { }

	@Override
	public void readStats(@NotNull IxyStats stats) { }

	@Override
	public boolean isPromiscuous() {
		return false;
	}

	@Override
	public void enablePromiscuous() { }

	@Override
	public void disablePromiscuous() { }

	@Override
	public long getLinkSpeed() {
		return 0;
	}

	@Override
	public int rxBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		return 0;
	}

	@Override
	public int txBatch(int queue, @NotNull IxyPacketBuffer[] packets, int offset, int length) {
		return 0;
	}

}
