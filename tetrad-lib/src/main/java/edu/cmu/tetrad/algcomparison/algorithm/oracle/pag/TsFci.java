package edu.cmu.tetrad.algcomparison.algorithm.oracle.pag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.TakesIndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.TakesInitialGraph;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.TsDagToPag;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.algo.subsampling.GeneralSubSamplingTest;
import edu.pitt.dbmi.algo.subsampling.SubSamplingEdgeEnsemble;

import java.util.List;

/**
 * tsFCI.
 *
 * @author jdramsey
 * @author dmalinsky
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "TsFCI",
        command = "ts-fci",
        algoType = AlgType.allow_latent_common_causes
)
public class TsFci implements Algorithm, TakesInitialGraph, HasKnowledge, TakesIndependenceWrapper {

    static final long serialVersionUID = 23L;
    private IndependenceWrapper test;
    private Algorithm algorithm = null;
    private Graph initialGraph = null;
    private IKnowledge knowledge = null;

    public TsFci() {
    }

    public TsFci(IndependenceWrapper test) {
        this.test = test;
    }

    public TsFci(IndependenceWrapper type, Algorithm algorithm) {
        this.test = type;
        this.algorithm = algorithm;
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
//    	if (!(dataSet instanceof TimeSeriesData)) {
//            throw new IllegalArgumentException("You need a (labeled) time series data set to run TsFCI.");
//        }
        
        if (parameters.getInt("numberSubSampling") < 1) {
        	if(knowledge != null) {
        		dataSet.setKnowledge(knowledge);
        	}
            edu.cmu.tetrad.search.TsFci search = new edu.cmu.tetrad.search.TsFci(test.getTest(dataSet, parameters));
            search.setDepth(parameters.getInt("depth"));
            search.setKnowledge(dataSet.getKnowledge());
            search.setVerbose(parameters.getBoolean("verbose"));
            
            return search.search();
        } else {
            TsFci tsFci = new TsFci(test, algorithm);

            DataSet data = (DataSet) dataSet;
            GeneralSubSamplingTest search = new GeneralSubSamplingTest(data, tsFci, parameters.getInt("numberSubSampling"));
            search.setKnowledge(knowledge);

            search.setSubSampleSize(parameters.getInt("subSampleSize"));
            search.setSubSamplingWithReplacement(parameters.getBoolean("subSamplingWithReplacement"));
            
            SubSamplingEdgeEnsemble edgeEnsemble = SubSamplingEdgeEnsemble.Highest;
            switch (parameters.getInt("subSamplingEnsemble", 1)) {
                case 0:
                    edgeEnsemble = SubSamplingEdgeEnsemble.Preserved;
                    break;
                case 1:
                    edgeEnsemble = SubSamplingEdgeEnsemble.Highest;
                    break;
                case 2:
                    edgeEnsemble = SubSamplingEdgeEnsemble.Majority;
            }
            search.setEdgeEnsemble(edgeEnsemble);
            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean("verbose"));
            return search.search();
        }
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return new TsDagToPag(new EdgeListGraph(graph)).convert();
    }

    public String getDescription() {
        return "tsFCI (Time Series Fast Causal Inference) using " + test.getDescription()
                + (algorithm != null ? " with initial graph from "
                        + algorithm.getDescription() : "");
    }

    @Override
    public DataType getDataType() {
        return test.getDataType();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = test.getParameters();
        // Subsampling
        parameters.add("numberSubSampling");
        parameters.add("subSampleSize");
        parameters.add("subSamplingWithReplacement");
        parameters.add("subSamplingEnsemble");
        parameters.add("verbose");
        return parameters;
    }

    @Override
    public IKnowledge getKnowledge() {
        return this.knowledge;
    }

    @Override
    public void setKnowledge(IKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    @Override
    public Graph getInitialGraph() {
        return initialGraph;
    }

    @Override
    public void setInitialGraph(Graph initialGraph) {
        this.initialGraph = initialGraph;
    }

    @Override
    public void setInitialGraph(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void setIndependenceWrapper(IndependenceWrapper test) {
        this.test = test;
    }

}
