package com.slytechs.jnet.jnetruntime.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Class Group.
 *
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 */
public class NetProcessorGroup extends AbstractNetProcessor<NetProcessorGroup> {

	/**
	 * The Class Empty.
	 */
	private static class Empty extends NetProcessorGroup {

		/** The Constant EMPTY. */
		private final static NetProcessor<?>[] EMPTY = new NetProcessor[0];

		/**
		 * Instantiates a new empty.
		 *
		 * @param pipeline the pipeline
		 * @param type     the type
		 */
		protected Empty(NetPipeline pipeline, NetProcessorType type) {
			super(pipeline, type);
		}

		/**
		 * Checks if is empty.
		 *
		 * @return true, if is empty
		 * @see com.slytechs.jnet.protocol.NetProcessorGroup#isEmpty()
		 */
		@Override
		public boolean isEmpty() {
			return true;
		}

		/**
		 * Processors.
		 *
		 * @return the net processor[]
		 * @see com.slytechs.jnet.protocol.NetProcessorGroup#processors()
		 */
		@Override
		public NetProcessor<?>[] processors() {
			return EMPTY;
		}

		/**
		 * Source.
		 *
		 * @return the object
		 * @see com.slytechs.jnet.protocol.NetProcessorGroup#sink()
		 */
		@Override
		public Object sink() {
			throw new IllegalStateException("no processors in group");
		}

		/**
		 * To array.
		 *
		 * @return the net processor[]
		 * @see com.slytechs.jnet.protocol.NetProcessorGroup#toArray()
		 */
		@Override
		public NetProcessor<?>[] toArray() {
			return EMPTY;
		}

	}
	
	public static List<NetProcessorGroup> all(NetPipeline parent) {
		var list = new ArrayList<NetProcessorGroup>();
		
		for (var t: NetProcessorType.values())
			list.add(new NetProcessorGroup(parent, t));
		
		return list;
	}

	/**
	 * Empty.
	 *
	 * @param pipeline the pipeline
	 * @param type     the type
	 * @return the group
	 */
	public static final NetProcessorGroup empty(NetPipeline pipeline, NetProcessorType type) {
		return new Empty(pipeline, type);
	}

	/** The processors. */
	private NetProcessor<?>[] processors;
	private NetProcessorGroup parent;

	/**
	 * New root group.
	 *
	 * @param pipeline the pipeline
	 * @param type     the type
	 */
	NetProcessorGroup(NetProcessorType type) {
		super(type.id(), type);
		this.parent = null;
	}

	/**
	 * New hierachal sub-group with a parent group.
	 *
	 * @param parent a parent group of this sub-group
	 * @param type     the group type based on processor type
	 */
	NetProcessorGroup(NetProcessorGroup parent, NetProcessorType type) {
		super(type.id(), type);
		this.parent = Objects.requireNonNull(parent, "parent");
	}

	/**
	 * Dispose.
	 *
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#dispose()
	 */
	@Override
	public void dispose() {
		for (var p : processors)
			p.dispose();
		
		processors = null;
		parent = null;
	}

	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		return processors.length == 0;
	}

	/**
	 * Processors.
	 *
	 * @return the net processor[]
	 */
	public NetProcessor<?>[] processors() {
		return this.processors;
	}

	/**
	 * Setup.
	 *
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#setup()
	 */
	@Override
	public void setup() {
		for (var p : processors)
			p.setup();
	}

	/**
	 * Source.
	 *
	 * @return the object
	 * @see com.slytechs.jnet.jnetruntime.pipeline.NetProcessor#sink()
	 */
	@Override
	public Object sink() {
		return processors[0].sink();
	}

	/**
	 * To array.
	 *
	 * @return the net processor[]
	 */
	public NetProcessor<?>[] toArray() {
		NetProcessor<?>[] array = pipeline().stream(type()).filter(NetProcessor::isEnabled).sorted()
				.toArray(NetProcessor[]::new);

		return array;
	}

	private NetProcessor<?> last() {
		if (processors.length == 0)
			throw new IllegalStateException("no processors in the group");

		NetProcessor<?> last = processors[processors.length - 1];

		return last;
	}

	 void linkGroup() {
		for (int i = processors.length - 2; i >= 0; i--) {
			var next = processors[i + 1];
			var curr = processors[i + 0];

			curr.link(next);
		}
	}

	@Override
	public void link(NetProcessor<?> next) {
		last().link(next);
	}

	@Override
	public NetPipeline pipeline() {
		return parent.pipeline();
	}

	@Override
	public NetProcessorGroup group() {
		return parent == null ? this : parent;
	}

	public boolean isRoot() {
		return group() == this;
	}

	NetProcessorGroup resolveByType(NetProcessorType type) {
		return this;
	}
}