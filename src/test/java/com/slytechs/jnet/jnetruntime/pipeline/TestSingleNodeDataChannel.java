/*
 * Sly Technologies Free License
 * 
 * Copyright 2023 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.jnetruntime.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * @author Sly Technologies Inc
 * @author repos@slytechs.com
 *
 */
class TestSingleNodeDataChannel {

	public enum TestType implements DataType {

		STRING,
		INTEGER,
		;

		private final DataSupport<?> dataSupport;

		@SuppressWarnings({ "rawtypes",
				"unchecked" })
		<T, U> TestType() {
			Class<T> arrayFactory = (Class<T>) Consumer.class;
			Function<Consumer[], Consumer> arrayHandler = (arr) -> {
				if (arr.length == 1)
					return arr[0];

				return v -> {
					for (var a : arr)
						a.accept(v);
				};
			};

			this.dataSupport = new DataSupport<T>(this, arrayFactory, (Function) arrayHandler, null);
		}

		/**
		 * @see com.slytechs.jnet.jnetruntime.pipeline.DataType#dataSupport()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T> DataSupport<T> dataSupport() {
			return (DataSupport<T>) dataSupport;
		}
	}

	final Consumer<String> INLINE = new Consumer<String>() {

		@Override
		public void accept(String input) {
			Integer inlineResult = Integer.parseInt(input, 16);

			if (OUTPUT != null)
				OUTPUT.accept(inlineResult);
		}

	};

	private Consumer<Integer> OUTPUT;

	@AfterEach
	void resetOutput() {
		OUTPUT = null;
	}

	private DataChannel<Consumer<String>, Consumer<Integer>> node(String name) {
		var node = new DataChannel<Consumer<String>, Consumer<Integer>>(name, TestType.STRING, TestType.INTEGER,
				INLINE);
		node.updateListener(out -> OUTPUT = out);

		return node;
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#DataChannel(com.slytechs.jnet.jnetruntime.pipeline.DataType, com.slytechs.jnet.jnetruntime.pipeline.DataType, java.lang.Object)}.
	 */
	@Test
	void testDataChannel() {

		var dc = new DataChannel<Consumer<String>, Consumer<Integer>>("", TestType.STRING, TestType.INTEGER, INLINE);

		assertNotNull(dc);
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#isActive()}.
	 */
	@Test
	void testIsActive() {

		var dc = node("node");
		dc.sink(System.out::println);

		assertTrue(dc.isActive(), "isActive");
		assertEquals(INLINE, dc.input(), "input"); // Because we have at least 1 output
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#isActive()}.
	 */
	@Test
	void testIsActive_FALSE() {

		var dc = node("node");

		assertFalse(dc.isActive(), "isActive");
		assertNull(dc.input(), "input"); // Since no outputs
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#connect(com.slytechs.jnet.jnetruntime.pipeline.DataChannel)}.
	 */
	@Test
	void testConnectDownstreamDataChannelOfT2Q() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#sink(java.lang.Object)}.
	 */
	@Test
	void testConnectDownstreamT2() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#enable(boolean)}.
	 */
	@Test
	void testEnableBypass() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#inputType()}.
	 */
	@Test
	void testInputType() {
		var node = node("node");

		assertEquals(TestType.STRING, node.inputType(), "inputType");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#isEnabled()}.
	 */
	@Test
	void testIsBypassed() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#onConnection(com.slytechs.jnet.jnetruntime.pipeline.DataChannel.Connection)}.
	 */
	@Test
	void testOnUpstreamLink() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#output()}.
	 */
	@Test
	void testOutput(TestInfo info) {
		var node = node("node");

		node.sink(System.out::println);

		System.out.printf("-------- %s --------%n", info.getDisplayName());
		node.input().accept("1a");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#outputType()}.
	 */
	@Test
	void testOutputType() {
		var node = node("node");

		assertEquals(TestType.INTEGER, node.outputType(), "outputType");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#updateListener(java.util.function.Consumer)}.
	 */
	@Test
	void testUpdateListener(TestInfo info) {
		
		final String node1Name = "node1";
		final String node2Name = DataChannel.class.getSimpleName();
		final String node3Name = "node3";

		Consumer<Integer> NOOP = i -> {};

		var n1 = node(node1Name);
		var n2 = new DataChannel<Consumer<Integer>, Consumer<Integer>>(node2Name,
				TestType.INTEGER,
				TestType.INTEGER,
				NOOP);
		var n3 = new DataChannel<Consumer<Integer>, Consumer<Integer>>(node3Name,
				TestType.INTEGER,
				TestType.INTEGER,
				NOOP);
		
		n3.sink(i -> {});

		n1.connect(n2);
		n1.connect(n3);

		System.out.printf("-------- %s --------%n", info.getDisplayName());
		System.out.println(n1);

//		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link com.slytechs.jnet.jnetruntime.pipeline.DataChannel#input()}.
	 */
	@Test
	void testInput() {
		fail("Not yet implemented");
	}

}
