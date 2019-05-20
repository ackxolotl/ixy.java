package de.tum.in.net.ixy.memory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a memory region reserved for a specific number of {@link PacketBuffer}s.
 * <p>
 * This class implements the {@link Comparable} and {@link Collection} interfaces to better integrate with Java
 * ecosystem, although it is not entirely compliant. This class only implements the methods that allow adding elements,
 * that is to say, the methods that allow to add back again the free {@link PacketBuffers}.
 * <p>
 * The main goal of this class is to enable easy management of {@link PacketBuffer} instances.
 */
@Slf4j
public final class MemoryPool implements Comparable<MemoryPool>, Collection<PacketBuffer> {

	/** Stores all the memory pools ever created. */
	public static final TreeMap<Integer, MemoryPool> pools = new TreeMap<>();

	/**
	 * Given an {@code id}, computes one that is not already being used by another memory pool.
	 * 
	 * @param id The initial id to be used.
	 * @return An id that is not being used.
	 */
	private static int getValidId(int id) {
		while (pools.containsKey(id)) {
			id += 1;
		}
		if (BuildConstants.DEBUG) log.trace("Found a valid memory pool id {}", id);
		return id;
	}

	/**
	 * Adds a memory pool to {@link #pools}.
	 * <p>
	 * If the memory pool id is already in the {@link #pools} collection, then a new id is generated and the instance
	 * is updated.
	 * 
	 * @param mempool The memory pool to add.
	 * @see MemoryPool#getValidId(int)
	 */
	public static boolean addMempool(final MemoryPool mempool) {
		if (BuildConstants.DEBUG) log.trace("Adding a memory pool");
		if (mempool == null) {
			return false;
		}
		val id = getValidId(mempool.id);
		mempool.id = id;
		pools.put(id, mempool);
		return true;
	}

	/** The unique identifier of the memory pool. */
	@Getter
	private int id;

	/** The base address of the memory pool. */
	@Getter
	private long baseAddress;

	/** The size of the packet buffers. */
	@Getter
	private int packetBufferSize;

	/** The number of entries the pool has. */
	@Getter
	private int entrySize;

	/** Bouble ended queue with a bunch a pre-allocated {@link PacketBuffer} instances. */
	private Deque<PacketBuffer> buffers;

	/**
	 * Creates an instance an empty instance.
	 * <p>
	 * In order to use the instance, all the {@link PacketBuffer}s must be allocated.
	 * <p>
	 * This constructor automatically keeps track of the created instances by adding them to the list {@link #pools}.
	 * 
	 * @param address The base address of the memory pool.
	 * @param size    The size of each buffer inside the memory pool.
	 * @param entries The number of entries to allocate.
	 * @see #allocate()
	 */
	public MemoryPool(final long address, final int size, final int entries) {
		if (BuildConstants.DEBUG) {
			val xaddress = Long.toHexString(address);
			log.trace("Creating memory pool with {} entries, a total size of {} bytes @ 0x{}", entries, size, xaddress);
		}
		baseAddress = address;
		packetBufferSize = size;
		entrySize = entries;
		id = pools.isEmpty() ? 0 : getValidId(pools.lastKey());
		pools.put(id, this);
		if (BuildConstants.DEBUG) log.info("There are {} memory pools", pools.size());
	}

	/** Pre-allocates all the {@link PacketBuffer}s. */
	public void allocate() {
		if (BuildConstants.DEBUG) log.trace("Allocating packet buffers");
		buffers = new ArrayDeque<PacketBuffer>(entrySize);
		for (var i = 0; i < entrySize; i += 1) {
			val virt = baseAddress + i * packetBufferSize;
			val buffer = new PacketBuffer(virt);
			buffer.setPhysicalAddress(MemoryUtils.virt2phys(virt));
			buffer.setSize(0);
			buffers.push(buffer);
		}
	}

	/**
	 * Returns a free pre-allocated packet buffer instance.
	 * <p>
	 * If there are no more free {@link PacketBuffer}s available, then an empty instance is returned.
	 * 
	 * @return A free packet buffer or a dummy instance.
	 * @see PacketBuffer#empty()
	 */
	@NotNull
	public PacketBuffer pop() {
		if (BuildConstants.DEBUG) {
			log.trace("Obtaining a free packet buffer");
			if (buffers.isEmpty()) {
				log.warn("There are no free packet buffers, a dummy packet buffer will be returned");
				return PacketBuffer.empty();
			}
			return buffers.pop();
		} else {
			return buffers.isEmpty() ? PacketBuffer.empty() : buffers.pop();
		}
	}

	//////////////////////////////////////////////// OVERRIDEN METHODS /////////////////////////////////////////////////

	/** {@inheritDoc} */
	@Override
	public int compareTo(@NonNull final MemoryPool mempool) {
		if (BuildConstants.DEBUG) log.trace("Comparing with another MemoryPool");
		return id - mempool.id;
	}

	/** {@inheritDoc} */
	public int size() {
		return buffers.size();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmpty() {
		if (BuildConstants.DEBUG) log.trace("Checking if MemoryPool is empty");
		return buffers.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean add(final PacketBuffer packetBuffer) {
		if (BuildConstants.DEBUG) log.trace("Adding PacketBuffer to the memory pool");
		if (packetBuffer == null) {
			if (BuildConstants.DEBUG) log.warn("Skipping invalid PacketBuffer that is null");
			return false;
		} else if (buffers.size() >= entrySize) {
			if (BuildConstants.DEBUG) log.warn("Memory pool queue is already full, cannot add free PacketBuffer");
			return false;
		}
		buffers.add(packetBuffer);
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean addAll(final Collection<? extends PacketBuffer> packetBuffers) {
		if (BuildConstants.DEBUG) log.trace("Adding more than one PacketBuffer to the memory pool");
		if (packetBuffers == null) {
			return false;
		}
		val amount = packetBuffers.size();
		val size = buffers.size();
		if (amount <= 0) {
			return false;
		} else if (size >= entrySize) {
			if (BuildConstants.DEBUG) log.warn("Memory pool queue is already full, cannot add any free PacketBuffer");
			return false;
		}
		var remaining = entrySize - size;
		if (BuildConstants.DEBUG && amount > remaining) {
			log.warn("Memory pool queue will be full, cannot add all free PacketBuffers");
		}
		for (val packetBuffer : packetBuffers) {
			if (packetBuffer != null) {
				buffers.add(packetBuffer);
				if (--remaining == 0) break;
			} else if (BuildConstants.DEBUG) {
				log.warn("Skipping invalid PacketBuffer that is null");
			}
		}
		return size != buffers.size();
	}


	/** {@inheritDoc} */
	@Override
	public Iterator<PacketBuffer> iterator() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public void forEach(Consumer<? super PacketBuffer> action) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
    public Spliterator<PacketBuffer> spliterator() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}


	/** {@inheritDoc} */
	@Override
	public boolean contains(final Object o) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public boolean containsAll(final Collection<?> c) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public <T> T[] toArray(final T[] a) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	@Override
	public <T> T[] toArray(final IntFunction<T[]> generator) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeIf(Predicate<? super PacketBuffer> filter) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeAll(final Collection<?> packetBuffers) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public boolean retainAll(final Collection<?> packetBuffers) {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

	/** {@inheritDoc} */
	@Override
	public Stream<PacketBuffer> stream() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}
	
	/** {@inheritDoc} */
	@Override
	public Stream<PacketBuffer> parallelStream() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
    }

	/** {@inheritDoc} */
	@Override
	public void clear() {
		throw new UnsupportedOperationException("only methods that return or add packet buffers are implemented");
	}

}