package edu.cmu.tetrad.algcomparison.algorithm.multi;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.MultiDataSetAlgorithm;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.algo.subsampling.GeneralSubSamplingTest;
import edu.pitt.dbmi.algo.subsampling.SubSamplingEdgeEnsemble;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Requires that the parameter 'randomSelectionSize' be set to indicate how many
 * datasets should be taken at a time (randomly). This cannot given multiple
 * values.
 *
 * @author jdramsey
 */
public class FgesConcatenated implements MultiDataSetAlgorithm, HasKnowledge {
	static final long serialVersionUID = 23L;
	private ScoreWrapper score;
	private IKnowledge knowledge = new Knowledge2();
	private IndependenceWrapper test;
	private Algorithm initialGraph = null;
	private boolean compareToTrue = false;

	public FgesConcatenated(ScoreWrapper score) {
		this.score = score;
	}

	public FgesConcatenated(ScoreWrapper score, boolean compareToTrue) {
		this.score = score;
		this.compareToTrue = compareToTrue;
	}

	public FgesConcatenated(ScoreWrapper score, Algorithm initialGraph) {
		this.score = score;
		this.initialGraph = initialGraph;
	}

	@Override
	public Graph search(List<DataModel> dataModels, Parameters parameters) {
		if (parameters.getInt("numberSubSampling") < 1) {
			List<DataSet> dataSets = new ArrayList<>();

			for (DataModel dataModel : dataModels) {
				dataSets.add((DataSet) dataModel);
			}

			DataSet dataSet = DataUtils.concatenate(dataSets);

			Graph initial = null;
			if (initialGraph != null) {

				initial = initialGraph.search(dataSet, parameters);
			}

			edu.cmu.tetrad.search.Fges search = new edu.cmu.tetrad.search.Fges(score.getScore(dataSet, parameters));
			search.setFaithfulnessAssumed(parameters.getBoolean("faithfulnessAssumed"));
			search.setKnowledge(knowledge);
			search.setVerbose(parameters.getBoolean("verbose"));
			search.setMaxDegree(parameters.getInt("maxDegree"));

			Object obj = parameters.get("printStedu.cmream");
			if (obj instanceof PrintStream) {
				search.setOut((PrintStream) obj);
			}

			if (initial != null) {
				search.setInitialGraph(initial);
			}

			return search.search();
		} else {
			FgesConcatenated fgesConcatenated = new FgesConcatenated(score, initialGraph);
			fgesConcatenated.setCompareToTrue(compareToTrue);
			//fgesConcatenated.setKnowledge(knowledge);

			List<DataSet> datasets = new ArrayList<>();

			for (DataModel dataModel : dataModels) {
				datasets.add((DataSet) dataModel);
			}
			GeneralSubSamplingTest search = new GeneralSubSamplingTest(datasets, fgesConcatenated, parameters.getInt("numberSubSampling"));
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
	public Graph search(DataModel dataSet, Parameters parameters) {
		if (parameters.getInt("numberSubSampling") < 1) {
			return search(Collections.singletonList((DataModel) DataUtils.getContinuousDataSet(dataSet)), parameters);
		} else {
			FgesConcatenated fgesConcatenated = new FgesConcatenated(score, initialGraph);
			fgesConcatenated.setCompareToTrue(compareToTrue);
			fgesConcatenated.setKnowledge(knowledge);

			List<DataSet> dataSets = Collections.singletonList(DataUtils.getContinuousDataSet(dataSet));
			GeneralSubSamplingTest search = new GeneralSubSamplingTest(dataSets, fgesConcatenated, parameters.getInt("numberSubSampling"));
			
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
		if (compareToTrue) {
			return new EdgeListGraph(graph);
		} else {
			return SearchGraphUtils.patternForDag(new EdgeListGraph(graph));
		}
	}

	@Override
	public String getDescription() {
		return "FGES (Fast Greedy Equivalence Search) on concatenated data using " + score.getDescription();
	}

	@Override
	public DataType getDataType() {
		return DataType.Continuous;
	}

	@Override
	public List<String> getParameters() {
		List<String> parameters = new ArrayList<>();
		parameters.add("faithfulnessAssumed");
		parameters.add("maxDegree");
		parameters.add("verbose");

		parameters.add("numRuns");
		parameters.add("randomSelectionSize");
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
		return knowledge;
	}

	@Override
	public void setKnowledge(IKnowledge knowledge) {
		this.knowledge = knowledge;
	}

	/**
	 * @param compareToTrue
	 *            true if the result should be compared to the true graph, false
	 *            if to the pattern of the true graph.
	 */
	public void setCompareToTrue(boolean compareToTrue) {
		this.compareToTrue = compareToTrue;
	}
}
