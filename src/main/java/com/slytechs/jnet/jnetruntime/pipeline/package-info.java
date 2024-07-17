/**
 * Provides a comprehensive API for processing data using a flexible and
 * extensible pipeline architecture. This package enables the creation of
 * complex data processing workflows by connecting various processors and
 * sub-pipelines in a hierarchical structure.
 * 
 * <h2>Pipeline Architecture Overview</h2>
 * <p>
 * A pipeline is composed of interconnected processor groups and sub-pipelines.
 * Data flows through the pipeline, undergoing transformations and analyses at
 * each stage. The architecture is designed to be modular, allowing for easy
 * addition, removal, or modification of processing components.
 * </p>
 * 
 * <h3>Key Components</h3>
 * <ul>
 * <li><strong>Pipeline:</strong> The main container and manager for the entire
 * processing workflow.</li>
 * <li><strong>Processor Groups:</strong> Collections of processors that handle
 * a specific data type transformation.</li>
 * <li><strong>Processors:</strong> Individual units that perform specific
 * operations on the data.</li>
 * <li><strong>Data Channels:</strong> Conduits that facilitate data flow
 * between processors and groups.</li>
 * </ul>
 * 
 * <h2>Data Flow Through the Pipeline</h2>
 * <p>
 * Data traverses the pipeline through a series of interconnected data channels.
 * Each channel represents a typed connection between processors or processor
 * groups. The flow can be visualized as follows:
 * </p>
 * 
 * <pre>
 * [Input] -> [Processor Group A] -> [Processor Group B] -> [Sub-Pipeline X] -> [Processor Group C] -> [Output]
 *                |                        |                       |                    |
 *                v                        v                       v                    v
 *           [Processor A1]           [Processor B1]        [Processor X1]        [Processor C1]
 *           [Processor A2]           [Processor B2]        [Processor X2]        [Processor C2]
 *                                                          [Processor X3]
 * </pre>
 * 
 * <ol>
 * <li><strong>Input Ingestion:</strong> Data enters the pipeline through a
 * defined input channel.</li>
 * <li><strong>Processor Group Processing:</strong> The data is passed through
 * relevant processor groups. Each group may contain multiple processors that
 * operate on the data sequentially or in parallel.</li>
 * <li><strong>Inter-Group Data Flow:</strong> Output from one processor group
 * becomes input for the next compatible group.</li>
 * <li><strong>Sub-Pipeline Integration:</strong> Data may be routed through
 * sub-pipelines, which are self-contained processing units with their own
 * groups and processors.</li>
 * <li><strong>Output Generation:</strong> Processed data is emitted through
 * output channels, which can be consumed by the application or further
 * pipelines.</li>
 * </ol>
 * 
 * <h2>Data Channel Dynamics</h2>
 * <p>
 * Data channels play a crucial role in managing the flow of information:
 * </p>
 * <ul>
 * <li><strong>Type Safety:</strong> Channels ensure type compatibility between
 * connected components.</li>
 * <li><strong>Dynamic Routing:</strong> Based on data types, channels can
 * dynamically route data to appropriate processors.</li>
 * <li><strong>Buffering:</strong> Channels may implement buffering to manage
 * flow control between fast and slow processors.</li>
 * <li><strong>Multiplexing:</strong> A single output can be distributed to
 * multiple downstream processors via branching channels.</li>
 * </ul>
 * 
 * <h2>Processor and Group Lifecycle</h2>
 * <p>
 * Processors and groups can be dynamically managed within the pipeline:
 * </p>
 * <ul>
 * <li><strong>Installation:</strong> New processors or groups can be added
 * using {@link Pipeline#install} methods.</li>
 * <li><strong>Enabling/Disabling:</strong> Processors can be toggled on or off,
 * affecting data flow without removal.</li>
 * <li><strong>Uninstallation:</strong> Processors can be removed entirely,
 * triggering pipeline reconfiguration.</li>
 * <li><strong>Auto-Pruning:</strong> Inactive or disconnected components are
 * automatically removed during optimization.</li>
 * </ul>
 * 
 * <h2>Advanced Features</h2>
 * <p>
 * The pipeline architecture supports several advanced capabilities:
 * </p>
 * <ul>
 * <li><strong>Hierarchical Pipelines:</strong> Pipelines can be nested,
 * allowing for modular and reusable processing units.</li>
 * <li><strong>Dynamic Reconfiguration:</strong> The pipeline structure can be
 * modified at runtime for adaptive processing.</li>
 * <li><strong>Processor Contexts:</strong> Shared contexts allow for state
 * management and inter-processor communication.</li>
 * <li><strong>Extensibility:</strong> Custom processors and data types can be
 * easily integrated into existing pipelines.</li>
 * </ul>
 * 
 * <h2>Use Case Example: Network Packet Processing</h2>
 * <p>
 * A typical use case for this pipeline architecture is processing network
 * packets:
 * </p>
 * 
 * <pre>
 * [Raw Packet Data] -> [Packet Decoder] -> [IP Reassembler] -> [TCP Stream Reconstructor] -> [HTTP Parser] -> [Application Logic]
 *                           |                    |                       |                        |
 *                           v                    v                       v                        v
 *                    [Packet Filter]     [Decryption Engine]    [Flow Analyzer]           [Content Extractor]
 * </pre>
 * <p>
 * In this scenario, raw packet data flows through various processing stages,
 * each implemented as a processor or processor group. The modular nature allows
 * for easy insertion of additional processors (e.g., for logging, statistics
 * gathering, or protocol-specific analysis) at any point in the pipeline.
 * </p>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 * <li>Design processors to be stateless where possible, using the processor
 * context for shared state.</li>
 * <li>Utilize type-safe data channels to ensure data integrity throughout the
 * pipeline.</li>
 * <li>Implement efficient enable/disable mechanisms in processors to allow for
 * dynamic pipeline optimization.</li>
 * <li>Consider performance implications when designing complex pipeline
 * structures, especially with deep hierarchies.</li>
 * </ul>
 * 
 * @see Pipeline
 * @see Processor
 * @see ProcessorGroup
 * @see DataChannel
 * @see DataType
 */
package com.slytechs.jnet.jnetruntime.pipeline;