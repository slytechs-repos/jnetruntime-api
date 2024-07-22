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
/**
 * Net modules runtime utilties and APIs 
 */
module com.slytechs.jnet.jnetruntime {
	exports com.slytechs.jnet.jnetruntime;
	exports com.slytechs.jnet.jnetruntime.time;
	exports com.slytechs.jnet.jnetruntime.hash;
	exports com.slytechs.jnet.jnetruntime.util;
	exports com.slytechs.jnet.jnetruntime.pipeline;

	/* Private API */
	exports com.slytechs.jnet.jnetruntime.internal;
	exports com.slytechs.jnet.jnetruntime.internal.layout;
	exports com.slytechs.jnet.jnetruntime.internal.foreign;
	exports com.slytechs.jnet.jnetruntime.internal.foreign.struct;
	exports com.slytechs.jnet.jnetruntime.internal.concurrent;
	exports com.slytechs.jnet.jnetruntime.internal.util;
	exports com.slytechs.jnet.jnetruntime.internal.util.format;
	exports com.slytechs.jnet.jnetruntime.internal.util.function;
	exports com.slytechs.jnet.jnetruntime.internal.util.collection;
	exports com.slytechs.jnet.jnetruntime.internal.json;

	requires java.logging;

}