package com.github.tDBN.dbn;

import java.util.List;

public class MDLScoringFunction extends LLScoringFunction {

	@Override
	public double evaluate(Observations observations, int transition, List<Integer> parentNodesPast,
			Integer parentNodePresent, int childNode) {

		LocalConfiguration c = new LocalConfiguration(observations.getAttributes(), observations.getMarkovLag(),
				parentNodesPast, parentNodePresent, childNode);

		double score = super.evaluate(observations, transition, parentNodesPast, parentNodePresent, childNode);

		// regularizer term
		score -= 0.5 * Math.log(observations.numObservations(transition)) * c.getNumParameters();

		return score;
	}

}
