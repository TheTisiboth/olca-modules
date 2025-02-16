package org.openlca.core.results.providers;

import java.util.Arrays;

import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.results.SimpleResult;

/**
 * A SimpleResultProvider just wraps a set of result data. It
 * should be just used with the SimpleResult view as the
 * detailed contributions are not provided.
 */
public class SimpleResultProvider implements ResultProvider {

	private final TechIndex techIndex;
	private FlowIndex flowIndex;
	private ImpactIndex impactIndex;
	private double[] scalingVector;
	private double[] totalFlows;
	private double[] totalImpacts;
	private double[] totalRequirements;

	private SimpleResultProvider(TechIndex techIndex) {
		this.techIndex = techIndex;
	}

	public static SimpleResultProvider of(TechIndex techIndex) {
		return new SimpleResultProvider(techIndex);
	}

	public SimpleResultProvider withFlowIndex(FlowIndex flowIndex) {
		this.flowIndex = flowIndex;
		return this;
	}

	public SimpleResultProvider withImpactIndex(ImpactIndex impactIndex) {
		this.impactIndex = impactIndex;
		return this;
	}

	public SimpleResultProvider withScalingVector(double[] s) {
		this.scalingVector = s;
		return this;
	}

	public SimpleResultProvider withTotalRequirements(double[] t) {
		this.totalRequirements = t;
		return this;
	}

	public SimpleResultProvider withTotalFlows(double[] v) {
		this.totalFlows = v;
		return this;
	}

	public SimpleResultProvider withTotalImpacts(double[] v) {
		this.totalImpacts = v;
		return this;
	}

	public SimpleResult toResult() {
		return new SimpleResult(this);
	}

	@Override
	public TechIndex techIndex() {
		return techIndex;
	}

	@Override
	public FlowIndex flowIndex() {
		return flowIndex;
	}

	@Override
	public ImpactIndex impactIndex() {
		return impactIndex;
	}

	@Override
	public boolean hasCosts() {
		return false;
	}

	@Override
	public double[] scalingVector() {
		if (scalingVector != null)
			return scalingVector;
		if (techIndex == null)
			return EMPTY_VECTOR;
		var s = new double[techIndex.size()];
		Arrays.fill(s, 1);
		return s;
	}

	@Override
	public double[] totalRequirements() {
		if (totalRequirements != null)
			return totalRequirements;
		if (techIndex == null)
			return EMPTY_VECTOR;
		var t = new double[techIndex.size()];
		Arrays.fill(t, 1);
		return t;
	}

	@Override
	public double[] techColumnOf(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] solutionOfOne(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double loopFactorOf(int product) {
		return 0;
	}

	@Override
	public double[] unscaledFlowsOf(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] totalFlowsOfOne(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] totalFlows() {
		if (totalFlows != null)
			return totalFlows;
		return flowIndex == null
			? EMPTY_VECTOR
			: new double[flowIndex.size()];
	}

	@Override
	public double[] impactFactorsOf(int flow) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] directImpactsOf(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] totalImpactsOfOne(int product) {
		return EMPTY_VECTOR;
	}

	@Override
	public double[] totalImpacts() {
		if (totalImpacts != null)
			return totalImpacts;
		return impactIndex == null
			? EMPTY_VECTOR
			: new double[impactIndex.size()];
	}

	@Override
	public double directCostsOf(int product) {
		return 0;
	}

	@Override
	public double totalCostsOfOne(int product) {
		return 0;
	}

	@Override
	public double totalCosts() {
		return 0;
	}
}
