package org.deeplearning4j.nn.transferlearning;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.graph.GraphVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.FrozenLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;
import org.nd4j.linalg.primitives.Triple;

import java.util.*;

/**
 * The transfer learning API can be used to modify the architecture or the learning parameters of an existing multilayernetwork or computation graph.
 * It allows one to
 *  - change nOut of an existing layer
 *  - remove and add existing layers/vertices
 *  - fine tune learning configuration (learning rate, updater etc)
 *  - hold parameters for specified layers as a constant
 */
@Slf4j
public class TransferLearning {

    public static class Builder {
        private MultiLayerConfiguration origConf;
        private MultiLayerNetwork origModel;

        private MultiLayerNetwork editedModel;
        private FineTuneConfiguration finetuneConfiguration;
        private int frozenTill = -1;
        private int popN = 0;
        private boolean prepDone = false;
        private Set<Integer> editedLayers = new HashSet<>();
        private Map<Integer, Triple<Integer, Pair<WeightInit, Distribution>, Pair<WeightInit, Distribution>>> editedLayersMap =
                        new HashMap<>();
        private List<INDArray> editedParams = new ArrayList<>();
        private List<NeuralNetConfiguration> editedConfs = new ArrayList<>();
        private List<INDArray> appendParams = new ArrayList<>(); //these could be new arrays, and views from origParams
        private List<NeuralNetConfiguration> appendConfs = new ArrayList<>();
        private InputType inputType;

        /**
         * Multilayer Network to tweak for transfer learning
         * @param origModel
         */
        public Builder(MultiLayerNetwork origModel) {
            this.origModel = origModel;
            this.origConf = origModel.getLayerWiseConfigurations().clone();
        }

        /**
         * Fine tune configurations specified will overwrite the existing configuration if any
         * Usage example: specify a learning rate will set specified learning rate on all layers
         * Refer to the fineTuneConfiguration class for more details
         * @param finetuneConfiguration
         * @return Builder
         */
        public Builder fineTuneConfiguration(FineTuneConfiguration finetuneConfiguration) {
            this.finetuneConfiguration = finetuneConfiguration;
            return this;
        }

        /**
         * Specify a layer to set as a "feature extractor"
         * The specified layer and the layers preceding it will be "frozen" with parameters staying constant
         * @param layerNum
         * @return Builder
         */
        public Builder setFeatureExtractor(int layerNum) {
            this.frozenTill = layerNum;
            return this;
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         *
         * @param layerNum The index of the layer to change nOut of
         * @param nOut     Value of nOut to change to
         * @param scheme   Weight Init scheme to use for params in layernum and layernum+1
         * @return Builder
         */
        public Builder nOutReplace(int layerNum, int nOut, WeightInit scheme) {
            return nOutReplace(layerNum, nOut, scheme, scheme, null, null);
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         *
         * @param layerNum The index of the layer to change nOut of
         * @param nOut     Value of nOut to change to
         * @param dist     Distribution to use in conjunction with weight init DISTRIBUTION for params in layernum and layernum+1
         * @return Builder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public Builder nOutReplace(int layerNum, int nOut, Distribution dist) {
            return nOutReplace(layerNum, nOut, WeightInit.DISTRIBUTION, WeightInit.DISTRIBUTION, dist, dist);
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerNum   The index of the layer to change nOut of
         * @param nOut       Value of nOut to change to
         * @param scheme     Weight Init scheme to use for params in the layerNum
         * @param schemeNext Weight Init scheme to use for params in the layerNum+1
         * @return Builder
         */
        public Builder nOutReplace(int layerNum, int nOut, WeightInit scheme, WeightInit schemeNext) {
            return nOutReplace(layerNum, nOut, scheme, schemeNext, null, null);
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerNum The index of the layer to change nOut of
         * @param nOut     Value of nOut to change to
         * @param dist     Distribution to use for params in the layerNum
         * @param distNext Distribution to use for parmas in layerNum+1
         * @return Builder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public Builder nOutReplace(int layerNum, int nOut, Distribution dist, Distribution distNext) {
            return nOutReplace(layerNum, nOut, WeightInit.DISTRIBUTION, WeightInit.DISTRIBUTION, dist, distNext);
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerNum The index of the layer to change nOut of
         * @param nOut     Value of nOut to change to
         * @param scheme   Weight init scheme to use for params in layerNum
         * @param distNext Distribution to use for parmas in layerNum+1
         * @return Builder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public Builder nOutReplace(int layerNum, int nOut, WeightInit scheme, Distribution distNext) {
            return nOutReplace(layerNum, nOut, scheme, WeightInit.DISTRIBUTION, null, distNext);
        }

        /**
         * Modify the architecture of a layer by changing nOut
         * Note this will also affect the layer that follows the layer specified, unless it is the output layer
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerNum   The index of the layer to change nOut of
         * @param nOut       Value of nOut to change to
         * @param dist       Distribution to use for parmas in layerNum
         * @param schemeNext Weight init scheme to use for params in layerNum+1
         * @return Builder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public Builder nOutReplace(int layerNum, int nOut, Distribution dist, WeightInit schemeNext) {
            return nOutReplace(layerNum, nOut, WeightInit.DISTRIBUTION, schemeNext, dist, null);
        }

        private Builder nOutReplace(int layerNum, int nOut, WeightInit scheme, WeightInit schemeNext, Distribution dist,
                        Distribution distNext) {
            editedLayers.add(layerNum);
            Triple<Integer, Pair<WeightInit, Distribution>, Pair<WeightInit, Distribution>> t =
                            new Triple(nOut, new Pair<>(scheme, dist),
                                            new Pair<>(schemeNext, distNext));
            editedLayersMap.put(layerNum, t);
            return this;
        }

        /**
         * Helper method to remove the outputLayer of the net.
         * Only one of the two - removeOutputLayer() or removeLayersFromOutput(layerNum) - can be specified
         * When removing layers at the very least an output layer should be added with .addLayer(...)
         *
         * @return Builder
         */
        public Builder removeOutputLayer() {
            popN = 1;
            return this;
        }

        /**
         * Remove last "n" layers of the net
         * At least an output layer must be added back in
         * @param layerNum number of layers to remove
         * @return Builder
         */
        public Builder removeLayersFromOutput(int layerNum) {
            if (popN == 0) {
                popN = layerNum;
            } else {
                throw new IllegalArgumentException("Remove layers from can only be called once");
            }
            return this;
        }

        /**
         * Add layers to the net
         * Required if layers are removed. Can be called multiple times and layers will be added in the order with which they were called.
         * At the very least an outputLayer must be added (output layer should be added last - as per the note on order)
         * Learning configs (like updaters, learning rate etc) specified with the layer here will be honored
         *
         * @param layer layer conf to add (similar to the NeuralNetConfiguration .list().layer(...)
         * @return Builder
         */
        public Builder addLayer(Layer layer) {

            if (!prepDone) {
                doPrep();
            }

            // Use the fineTune config to create the required NeuralNetConfiguration + Layer instances
            //instantiate dummy layer to get the params

            //Build a nn config builder with settings from finetune. Set layer with the added layer
            //Issue: fine tune config has .learningRate(x), then I add a layer with .learningRate(y)...
            //We don't want that to be overridden
//            NeuralNetConfiguration layerConf =
//                            finetuneConfiguration.appliedNeuralNetConfigurationBuilder().layer(layer).build();
//
//            int numParams = layer.initializer().numParams(layerConf);
//            INDArray params;
//            if (numParams > 0) {
//                params = Nd4j.create(1, numParams);
//                String name = null;
//                if(layerConf.getLayer() != null){
//                    name = layerConf.getLayer().getLayerName();
//                }
//                org.deeplearning4j.nn.api.Layer someLayer = layer.instantiate(layerConf, null, name,0, 1, params, true);
//                appendParams.add(someLayer.params());
//                appendConfs.add(someLayer.conf());
//            } else {
//                appendConfs.add(layerConf);
//
//            }
//            return this;
            throw new RuntimeException("Not yet reimplemented");
        }

        public Builder setInputType(InputType inputType) {
            this.inputType = inputType;
            return this;
        }

        /**
         * Returns a model with the fine tune configuration and specified architecture changes.
         * .init() need not be called. Can be directly fit.
         *
         * @return MultiLayerNetwork
         */
        public MultiLayerNetwork build() {

            if (!prepDone) {
                doPrep();
            }

            editedModel = new MultiLayerNetwork(constructConf(), constructParams());
            if (frozenTill != -1) {
                org.deeplearning4j.nn.api.Layer[] layers = editedModel.getLayers();
                for (int i = frozenTill; i >= 0; i--) {
                    //Complication here: inner Layer (implementation) NeuralNetConfiguration.layer (config) should keep
                    // the original layer config. While network NNC should have the frozen layer, for to/from JSON etc
                    /*
                    NeuralNetConfiguration origNNC = editedModel.getLayerWiseConfigurations().getConf(i);
                    NeuralNetConfiguration layerNNC = origNNC.clone();
                    editedModel.getLayerWiseConfigurations().getConf(i).resetVariables();
                    layers[i].setConf(layerNNC);
                    layers[i] = new FrozenLayer(layers[i]);

                    if (origNNC.getVariables() != null) {
                        List<String> vars = origNNC.variables(true);
                        origNNC.clearVariables();
                        layerNNC.clearVariables();
                        for (String s : vars) {
                            origNNC.variables(false).add(s);
                            origNNC.getL1ByParam().put(s, 0.0);
                            origNNC.getL2ByParam().put(s, 0.0);

                            layerNNC.variables(false).add(s);
                            layerNNC.getL1ByParam().put(s, 0.0);
                            layerNNC.getL2ByParam().put(s, 0.0);
                        }
                    }



                    Layer origLayerConf = editedModel.getLayerWiseConfigurations().getConf(i).getLayer();
                    Layer newLayerConf = new org.deeplearning4j.nn.conf.layers.misc.FrozenLayer(origLayerConf);
                    newLayerConf.setLayerName(origLayerConf.getLayerName());
                    editedModel.getLayerWiseConfigurations().getConf(i).setLayer(newLayerConf);
                     */
                    throw new RuntimeException("Not yet reimplemented");
                }
                editedModel.setLayers(layers);
            }

            return editedModel;
        }

        private void doPrep() {
            //first set finetune configs on all layers in model
            fineTuneConfigurationBuild();

            //editParams gets original model params
            for (int i = 0; i < origModel.getnLayers(); i++) {
                if (origModel.getLayer(i).numParams() > 0) {
                    //dup only if params are there
                    editedParams.add(origModel.getLayer(i).params().dup());
                } else {
                    editedParams.add(origModel.getLayer(i).params());
                }
            }
            //apply changes to nout/nin if any in sorted order and save to editedParams
            if (!editedLayers.isEmpty()) {
                Integer[] editedLayersSorted = editedLayers.toArray(new Integer[editedLayers.size()]);
                Arrays.sort(editedLayersSorted);
                for (int i = 0; i < editedLayersSorted.length; i++) {
                    int layerNum = editedLayersSorted[i];
                    nOutReplaceBuild(layerNum, editedLayersMap.get(layerNum).getLeft(),
                                    editedLayersMap.get(layerNum).getMiddle(),
                                    editedLayersMap.get(layerNum).getRight());
                }
            }

            //finally pop layers specified
            int i = 0;
            while (i < popN) {
                Integer layerNum = origModel.getnLayers() - i;
                editedConfs.remove(editedConfs.size() - 1);
                editedParams.remove(editedParams.size() - 1);
                i++;
            }
            prepDone = true;

        }


        private void fineTuneConfigurationBuild() {
            throw new RuntimeException();
            /*
            for (int i = 0; i < origConf.getConfs().size(); i++) {
                NeuralNetConfiguration layerConf;
                if (finetuneConfiguration != null) {
                    NeuralNetConfiguration nnc = origConf.getConf(i).clone();
                    finetuneConfiguration.applyToNeuralNetConfiguration(nnc);
                    layerConf = nnc;
                } else {
                    layerConf = origConf.getConf(i).clone();
                }
                editedConfs.add(layerConf);
            }*/
        }

        private void nOutReplaceBuild(int layerNum, int nOut, Pair<WeightInit, Distribution> schemedist,
                        Pair<WeightInit, Distribution> schemedistNext) {
            throw new RuntimeException();
            /*
            NeuralNetConfiguration layerConf = editedConfs.get(layerNum);
            Layer layerImpl = layerConf.getLayer(); //not a clone need to modify nOut in place
            FeedForwardLayer layerImplF = (FeedForwardLayer) layerImpl;
            layerImplF.setWeightInit(schemedist.getLeft());
            layerImplF.setDist(schemedist.getRight());
            layerImplF.setNOut(nOut);
            int numParams = layerImpl.initializer().numParams(layerConf);
            INDArray params = Nd4j.create(1, numParams);
            String name = null;
            if(layerConf.getLayer() != null){
                name = layerConf.getLayer().getLayerName();
            }
            org.deeplearning4j.nn.api.Layer someLayer = layerImpl.instantiate(layerConf, null, name,0, 1, params, true);
            editedParams.set(layerNum, someLayer.params());

            if (layerNum + 1 < editedConfs.size()) {
                layerConf = editedConfs.get(layerNum + 1);
                layerImpl = layerConf.getLayer(); //modify in place
                layerImplF = (FeedForwardLayer) layerImpl;
                layerImplF.setWeightInit(schemedistNext.getLeft());
                layerImplF.setDist(schemedistNext.getRight());
                layerImplF.setNIn(nOut);
                numParams = layerImpl.initializer().numParams(layerConf);
                if (numParams > 0) {
                    params = Nd4j.create(1, numParams);
                    name = null;
                    if(layerConf.getLayer() != null){
                        name = layerConf.getLayer().getLayerName();
                    }
                    someLayer = layerImpl.instantiate(layerConf, null, name,0, 1, params, true);
                    editedParams.set(layerNum + 1, someLayer.params());
                }
            }
            */
        }

        private INDArray constructParams() {
            //some params will be null for subsampling etc
            INDArray keepView = null;
            for (INDArray aParam : editedParams) {
                if (aParam != null) {
                    if (keepView == null) {
                        keepView = aParam;
                    } else {
                        keepView = Nd4j.hstack(keepView, aParam);
                    }
                }
            }
            if (!appendParams.isEmpty()) {
                INDArray appendView = Nd4j.hstack(appendParams);
                return Nd4j.hstack(keepView, appendView);
            } else {
                return keepView;
            }
        }

        private MultiLayerConfiguration constructConf() {
            //use the editedConfs list to make a new config
            List<NeuralNetConfiguration> allConfs = new ArrayList<>();
            allConfs.addAll(editedConfs);
            allConfs.addAll(appendConfs);

            //Set default layer names, if not set - as per NeuralNetConfiguration.ListBuilder.build()
            for (int i = 0; i < allConfs.size(); i++) {
                if (allConfs.get(i).getLayer().getLayerName() == null) {
                    allConfs.get(i).getLayer().setLayerName("layer" + i);
                }
            }

            /*
            MultiLayerConfiguration conf = new MultiLayerConfiguration.Builder()
                            .setInputType(this.inputType).confs(allConfs).build();
            if (finetuneConfiguration != null) {
                finetuneConfiguration.applyToMultiLayerConfiguration(conf);
            }
            return conf;
            */
            throw new RuntimeException();
        }
    }

    public static class GraphBuilder {
        private ComputationGraph origGraph;
        private ComputationGraphConfiguration origConfig;

        private FineTuneConfiguration fineTuneConfiguration;
        private ComputationGraphConfiguration.GraphBuilder editedConfigBuilder;

        private String[] frozenOutputAt;
        private boolean hasFrozen = false;
        private Set<String> editedVertices = new HashSet<>();
        private WorkspaceMode workspaceMode;

        /**
         * Computation Graph to tweak for transfer learning
         * @param origGraph
         */
        public GraphBuilder(ComputationGraph origGraph) {
            this.origGraph = origGraph;
            this.origConfig = origGraph.getConfiguration().clone();
        }

        /**
         * Set parameters to selectively override existing learning parameters
         * Usage eg. specify a lower learning rate. This will get applied to all layers
         * @param fineTuneConfiguration
         * @return GraphBuilder
         */
        public GraphBuilder fineTuneConfiguration(FineTuneConfiguration fineTuneConfiguration) {
            this.fineTuneConfiguration = fineTuneConfiguration;
            this.editedConfigBuilder = new ComputationGraphConfiguration.GraphBuilder(origConfig,
                            fineTuneConfiguration.appliedNeuralNetConfigurationBuilder());


            Map<String, Layer> vertices = this.editedConfigBuilder.getVertices();
            for (Map.Entry<String, Layer> gv : vertices.entrySet()) {
                Layer lv = gv.getValue();
                Layer newLayer = lv.clone();
//                fineTuneConfiguration.applyToNeuralNetConfiguration(newLayer);
                vertices.put(gv.getKey(),newLayer);
                newLayer.setLayerName(gv.getKey());
                throw new RuntimeException();
            }
            return this;
        }

        /**
         * Specify a layer vertex to set as a "feature extractor"
         * The specified layer vertex and the layers on the path from an input vertex to it it will be "frozen" with parameters staying constant
         * @param layerName
         * @return Builder
         */
        public GraphBuilder setFeatureExtractor(String... layerName) {
            this.hasFrozen = true;
            this.frozenOutputAt = layerName;
            return this;
        }

        /**
         * Modify the architecture of a vertex layer by changing nOut
         * Note this will also affect the vertex layer that follows the layer specified, unless it is the output layer
         * Currently does not support modifying nOut of layers that feed into non-layer vertices like merge, subset etc
         * To modify nOut for such vertices use remove vertex, followed by add vertex
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerName The name of the layer to change nOut of
         * @param nOut      Value of nOut to change to
         * @param scheme    Weight init scheme to use for params in layerName and the layers following it
         * @return GraphBuilder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public GraphBuilder nOutReplace(String layerName, int nOut, WeightInit scheme) {
            return nOutReplace(layerName, nOut, scheme, scheme, null, null);
        }

        /**
         * Modify the architecture of a vertex layer by changing nOut
         * Note this will also affect the vertex layer that follows the layer specified, unless it is the output layer
         * Currently does not support modifying nOut of layers that feed into non-layer vertices like merge, subset etc
         * To modify nOut for such vertices use remove vertex, followed by add vertex
         * Can specify different weight init schemes for the specified layer and the layer that follows it.
         *
         * @param layerName The name of the layer to change nOut of
         * @param nOut      Value of nOut to change to
         * @param dist      Weight distribution scheme to use
         * @return GraphBuilder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public GraphBuilder nOutReplace(String layerName, int nOut, Distribution dist) {
            return nOutReplace(layerName, nOut, WeightInit.DISTRIBUTION, WeightInit.DISTRIBUTION, dist, dist);
        }

        /**
         * Modified nOut of specified layer. Also affects layers following layerName unless they are output layers
         * @param layerName The name of the layer to change nOut of
         * @param nOut      Value of nOut to change to
         * @param dist      Weight distribution scheme to use for layerName
         * @param distNext  Weight distribution scheme for layers following layerName
         * @return GraphBuilder
         * @see org.deeplearning4j.nn.weights.WeightInit DISTRIBUTION
         */
        public GraphBuilder nOutReplace(String layerName, int nOut, Distribution dist, Distribution distNext) {
            return nOutReplace(layerName, nOut, WeightInit.DISTRIBUTION, WeightInit.DISTRIBUTION, dist, distNext);
        }

        public GraphBuilder nOutReplace(String layerName, int nOut, WeightInit scheme, Distribution dist) {
            return nOutReplace(layerName, nOut, scheme, WeightInit.DISTRIBUTION, null, dist);
        }

        public GraphBuilder nOutReplace(String layerName, int nOut, Distribution dist, WeightInit scheme) {
            return nOutReplace(layerName, nOut, WeightInit.DISTRIBUTION, scheme, dist, null);
        }


        public GraphBuilder nOutReplace(String layerName, int nOut, WeightInit scheme, WeightInit schemeNext) {
            return nOutReplace(layerName, nOut, scheme, schemeNext, null, null);
        }

        private GraphBuilder nOutReplace(String layerName, int nOut, WeightInit scheme, WeightInit schemeNext,
                        Distribution dist, Distribution distNext) {
            initBuilderIfReq();

            /*
            NeuralNetConfiguration layerConf = origGraph.getLayer(layerName).conf();
            Layer layerImpl = layerConf.getLayer().clone();
            layerImpl.resetLayerDefaultConfig();
            FeedForwardLayer layerImplF = (FeedForwardLayer) layerImpl;
            layerImplF.setWeightInit(scheme);
            layerImplF.setDist(dist);
            layerImplF.setNOut(nOut);

            editedConfigBuilder.removeVertex(layerName, false);
            LayerVertex lv = (LayerVertex) origConfig.getVertices().get(layerName);
            String[] lvInputs = origConfig.getVertexInputs().get(layerName).toArray(new String[0]);
            editedConfigBuilder.addLayer(layerName, layerImpl, null, lvInputs);
            editedVertices.add(layerName);

            //collect other vertices that have this vertex as inputs
            List<String> fanoutVertices = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : origConfig.getVertexInputs().entrySet()) {
                String currentVertex = entry.getKey();
                if (!currentVertex.equals(layerName)) {
                    if (entry.getValue().contains(layerName)) {
                        fanoutVertices.add(currentVertex);
                    }
                }
            }

            //change nIn of fanout
            for (String fanoutVertexName : fanoutVertices) {
                layerConf = origGraph.getLayer(fanoutVertexName).conf();
                layerImpl = layerConf.getLayer().clone();
                layerImplF = (FeedForwardLayer) layerImpl;
                layerImplF.setWeightInit(schemeNext);
                layerImplF.setDist(distNext);
                layerImplF.setNIn(nOut);

                editedConfigBuilder.removeVertex(fanoutVertexName, false);
                lv = (LayerVertex) origConfig.getVertices().get(fanoutVertexName);
                lvInputs = origConfig.getVertexInputs().get(fanoutVertexName).toArray(new String[0]);
                editedConfigBuilder.addLayer(fanoutVertexName, layerImpl, null, lvInputs);
                editedVertices.add(fanoutVertexName);
            }
            return this;
            */
            throw new RuntimeException();
        }

        /**
         * Remove the specified vertex from the computation graph but keep it's connections.
         * Note the expectation here is to then add back another vertex with the same name or else the graph will be left in an invalid state
         * Possibly with references to vertices that no longer exist
         * @param outputName
         * @return
         */
        public GraphBuilder removeVertexKeepConnections(String outputName) {
            initBuilderIfReq();
            editedConfigBuilder.removeVertex(outputName, false);
            return this;
        }

        /**
         * Remove specified vertex and it's connections from the computation graph
         * @param vertexName
         * @return
         */
        public GraphBuilder removeVertexAndConnections(String vertexName) {
            initBuilderIfReq();
            editedConfigBuilder.removeVertex(vertexName, true);
            return this;
        }

        /**
         * Add a layer of the specified configuration to the computation graph
         * @param layerName
         * @param layer
         * @param layerInputs
         * @return
         */
        public GraphBuilder addLayer(String layerName, Layer layer, String... layerInputs) {
            initBuilderIfReq();
            editedConfigBuilder.addLayer(layerName, layer, null, layerInputs);
            editedVertices.add(layerName);
            return this;
        }

        /**
         * Add a layer with a specified preprocessor
         * @param layerName
         * @param layer
         * @param preProcessor
         * @param layerInputs
         * @return
         */
        public GraphBuilder addLayer(String layerName, Layer layer, InputPreProcessor preProcessor,
                        String... layerInputs) {
            initBuilderIfReq();
            editedConfigBuilder.addLayer(layerName, layer, preProcessor, layerInputs);
            editedVertices.add(layerName);
            return this;
        }

        /**
         * Add a vertex of the given configuration to the computation graph
         * @param vertexName
         * @param vertex
         * @param vertexInputs
         * @return
         */
        public GraphBuilder addVertex(String vertexName, GraphVertex vertex, String... vertexInputs) {
            initBuilderIfReq();
            editedConfigBuilder.addVertex(vertexName, vertex, vertexInputs);
            editedVertices.add(vertexName);
            return this;
        }

        /**
         * Set outputs to the computation graph, will add to ones that are existing
         * Also determines the order, like in ComputationGraphConfiguration
         * @param outputNames
         * @return
         */
        public GraphBuilder setOutputs(String... outputNames) {
            initBuilderIfReq();
            editedConfigBuilder.setOutputs(outputNames);
            return this;
        }

        private void initBuilderIfReq() {
            if (editedConfigBuilder == null) {
                //No fine tune config has been set. One isn't required, but we need one to create the editedConfigBuilder
                //So: create an empty finetune config, which won't override anything
                //but keep the seed
                fineTuneConfiguration(new FineTuneConfiguration.Builder()
                                .seed(origConfig.getSeed()).build());
            }
        }

        /**
         * Sets new inputs for the computation graph. This method will remove any
         * pre-existing inputs.
         * @param inputs String names of each graph input.
         * @return {@code GraphBuilder} instance.
         */
        public GraphBuilder setInputs(String... inputs) {
            editedConfigBuilder.setNetworkInputs(Arrays.asList(inputs));
            return this;
        }

        /**
         * Sets the input type of corresponding inputs.
         * @param inputTypes The type of input (such as convolutional).
         * @return {@code GraphBuilder} instance.
         */
        public GraphBuilder setInputTypes(InputType... inputTypes) {
            editedConfigBuilder.setInputTypes(inputTypes);
            return this;
        }

        public GraphBuilder addInputs(String... inputNames) {
            editedConfigBuilder.addInputs(inputNames);
            return this;
        }

        public GraphBuilder setWorkspaceMode(WorkspaceMode workspaceMode) {
            this.workspaceMode = workspaceMode;
            return this;
        }

        /**
         * Returns a computation graph build to specifications.
         * Init has been internally called. Can be fit directly.
         * @return Computation graph
         */
        public ComputationGraph build() {
            initBuilderIfReq();

            ComputationGraphConfiguration newConfig = editedConfigBuilder.build();
            if (this.workspaceMode != null)
                newConfig.setTrainingWorkspaceMode(workspaceMode);
            ComputationGraph newGraph = new ComputationGraph(newConfig);
            newGraph.init();

//            int[] topologicalOrder = newGraph.topologicalSortOrder();
            List<String> topologicalOrder = newGraph.topologicalSortOrder();
            Map<String,org.deeplearning4j.nn.api.Layer> vertices = newGraph.getVerticesMap();
            if (!editedVertices.isEmpty()) {
                //set params from orig graph as necessary to new graph
                for (int i = 0; i < topologicalOrder.size(); i++) {

                    org.deeplearning4j.nn.api.Layer layer = vertices.get(topologicalOrder.get(i));
                    String layerName = vertices.get(topologicalOrder.get(i)).getName();
                    int range = layer.numParams();
                    if (range <= 0)
                        continue; //some layers have no params
                    if (editedVertices.contains(layerName))
                        continue; //keep the changed params
                    layer.setParams(origGraph.getLayer(layerName).params().dup()); //copy over origGraph params
                }
            } else {
                newGraph.setParams(origGraph.params());
            }

            //Freeze layers as necessary. Note: we can't simply say "everything before frozen layer X needs to be frozen
            // also" as this won't always work. For example, in1->A->C, in2->B->C, freeze B; A shouldn't be frozen, even
            // if A is before B in the topological sort order.
            //How it should be handled: use the graph structure + topological sort order.
            // If a vertex is marked to be frozen: freeze it
            // Any descendants of a frozen layer should also be frozen
            if (hasFrozen) {

                //Store all frozen layers, and any vertices inheriting from said layers
                Set<String> allFrozen = new HashSet<>();
                Collections.addAll(allFrozen, frozenOutputAt);

                for (int i = topologicalOrder.size() - 1; i >= 0; i--) {
                    org.deeplearning4j.nn.api.Layer gv = vertices.get(topologicalOrder.get(i));
                    if (allFrozen.contains(gv.getName())) {
                        //Need to freeze this layer - both the layer implementation, and the layer configuration
                        org.deeplearning4j.nn.api.Layer l = gv;
                        if(!(gv instanceof FrozenLayer)) {
                            gv = new FrozenLayer(gv);
                            vertices.put(topologicalOrder.get(i), gv);
                        }

                        /*
                        //Freeze in the configuration
                        String layerName = gv.getName();
                        LayerVertex currLayerVertex = (LayerVertex) newConfig.getVertices().get(layerName);
                        if(!newConfig.getNetworkInputs().contains(layerName) && currLayerVertex.getLayerConf() != null) {
                            //Skip input vertices, and vertices without layer configurations...
                            Layer origLayerConf = currLayerVertex.getLayerConf().getLayer();
                            Layer newLayerConf = new org.deeplearning4j.nn.conf.layers.misc.FrozenLayer(origLayerConf);
                            newLayerConf.setLayerName(origLayerConf.getLayerName());
                            //Complication here(and reason for clone on next line): inner Layer (implementation)
                            // NeuralNetConfiguration.layer (config) should keep the original layer config. While network
                            // NNC should have the frozen layer
                            NeuralNetConfiguration newNNC = currLayerVertex.getLayerConf().clone();
                            currLayerVertex.setLayerConf(newNNC);
                            currLayerVertex.getLayerConf().setLayer(newLayerConf);

                            //Make sure the underlying layer doesn't change:
                            List<String> vars = currLayerVertex.getLayerConf().variables(true);
                            currLayerVertex.getLayerConf().clearVariables();
                            for (String s : vars) {
                                newNNC.variables(false).add(s);
                                newNNC.getL1ByParam().put(s, 0.0);
                                newNNC.getL2ByParam().put(s, 0.0);
                            }
                        }

                        //We also need to place the layer in the CompGraph Layer[] (replacing the old one)
                        //This could no doubt be done more efficiently
                        org.deeplearning4j.nn.api.Layer[] layers = newGraph.getLayers();
                        for (int j = 0; j < layers.length; j++) {
                            if (layers[j] == l) {
                                layers[j] = gv; //Place the new frozen layer to replace the original layer
                                break;
                            }
                        }
                        System.out.println();

                        //Also: mark any inputs to the current vertex to be frozen also
                        List<String> inputsToCurrent = newConfig.getVertexInputs().get(gv.getName());
                        if(inputsToCurrent != null && inputsToCurrent.size() > 0) {
                            allFrozen.addAll(inputsToCurrent);
                        }
                        */
                        throw new RuntimeException();
                    }
                }
                newGraph.initGradientsView();
            }
            return newGraph;
        }
    }
}