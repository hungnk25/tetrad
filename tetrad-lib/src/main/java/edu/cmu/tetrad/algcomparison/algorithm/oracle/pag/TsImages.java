package edu.cmu.tetrad.algcomparison.algorithm.oracle.pag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.MultiDataSetAlgorithm;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.UsesScoreWrapper;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.*;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.algo.bootstrap.BootstrapEdgeEnsemble;
import edu.pitt.dbmi.algo.bootstrap.GeneralBootstrapTest;
import java.util.ArrayList;
import java.util.List;

/**
 * tsIMaGES.
 *
 * @author jdramsey
 * @author Daniel Malinsky
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "TsImages",
        command = "ts-imgs",
        algoType = AlgType.forbid_latent_common_causes,
        description = "Short blurb goes here"
)
public class TsImages implements Algorithm, HasKnowledge, MultiDataSetAlgorithm, UsesScoreWrapper {

    static final long serialVersionUID = 23L;
    private ScoreWrapper score;
    private Algorithm initialGraph = null;
    private IKnowledge knowledge = null;

    public TsImages() {
    }

    public TsImages(ScoreWrapper score) {
        if (!(score instanceof SemBicScore || score instanceof BDeuScore)) {
            throw new IllegalArgumentException("Only SEM BIC score or BDeu score can be used with this, sorry.");
        }

        this.score = score;
    }

    @Override
    public Graph search(DataModel dataModel, Parameters parameters) {
        if (!parameters.getBoolean("bootstrapping")) {
            DataSet dataSet = (DataSet) dataModel;
            TsGFci search;
            Score score1 = score.getScore(dataSet, parameters);
            IndependenceTest test = new IndTestScore(score1);
            search = new TsGFci(test, score1);
            search.setKnowledge(dataSet.getKnowledge());
            return search.search();
        } else {
            TsImages algorithm = new TsImages(score);

            DataSet data = (DataSet) dataModel;
            GeneralBootstrapTest search = new GeneralBootstrapTest(data, algorithm, parameters.getInt("bootstrapSampleSize"));

            BootstrapEdgeEnsemble edgeEnsemble = BootstrapEdgeEnsemble.Highest;
            switch (parameters.getInt("bootstrapEnsemble", 1)) {
                case 0:
                    edgeEnsemble = BootstrapEdgeEnsemble.Preserved;
                    break;
                case 1:
                    edgeEnsemble = BootstrapEdgeEnsemble.Highest;
                    break;
                case 2:
                    edgeEnsemble = BootstrapEdgeEnsemble.Majority;
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
        return "tsFCI (Time Series Fast Causal Inference) using " + score.getDescription()
                + (initialGraph != null ? " with initial graph from "
                        + initialGraph.getDescription() : "");
    }

    @Override
    public DataType getDataType() {
        return score.getDataType();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = score.getParameters();
        // Bootstrapping
        parameters.add("bootstrapping");
        parameters.add("bootstrapSampleSize");
        parameters.add("bootstrapEnsemble");
        parameters.add("verbose");
        return score.getParameters();
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
    public Graph search(List<DataModel> dataSets, Parameters parameters) {
        List<DataModel> dataModels = new ArrayList<>();

        for (DataModel dataSet : dataSets) {
            dataModels.add(dataSet);
        }

        TsGFci search;

        if (score instanceof SemBicScore) {
            SemBicScoreImages gesScore = new SemBicScoreImages(dataModels);
            gesScore.setPenaltyDiscount(parameters.getDouble("penaltyDiscount"));
            IndependenceTest test = new IndTestScore(gesScore);
            search = new TsGFci(test, gesScore);
        } else if (score instanceof BDeScore) {
            double samplePrior = parameters.getDouble("samplePrior", 1);
            double structurePrior = parameters.getDouble("structurePrior", 1);
            BdeuScoreImages score = new BdeuScoreImages(dataModels);
            score.setSamplePrior(samplePrior);
            score.setStructurePrior(structurePrior);
            IndependenceTest test = new IndTestScore(score);
            search = new TsGFci(test, score);
        } else {
            throw new IllegalStateException("Sorry, data must either be all continuous or all discrete.");
        }

        IKnowledge knowledge = dataModels.get(0).getKnowledge();
        search.setKnowledge(knowledge);
        return search.search();
    }

    @Override
    public void setScoreWrapper(ScoreWrapper score) {
        this.score = score;
    }
}
