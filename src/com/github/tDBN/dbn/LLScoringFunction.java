package com.github.tDBN.dbn;

import java.util.List;

public class LLScoringFunction implements ScoringFunction {

	@Override
	public double evaluate(Observations observations, int transition, List<Integer> parentNodesPast, int childNode) {
		return evaluate(observations, transition, parentNodesPast, null, childNode);
	}

	@Override
	public double evaluate(Observations observations, int transition, List<Integer> parentNodesPast,
			Integer parentNodePresent, int childNode) {

		LocalConfiguration c = new LocalConfiguration(observations.getAttributes(), parentNodesPast, parentNodePresent,
				childNode);

		double score = 0;

		do {
			c.setConsiderChild(false);
			int Nij = observations.count(c, transition);
			c.setConsiderChild(true);
			do {
				int Nijk = observations.count(c, transition);
				if (Nijk != 0 && Nijk != Nij) {
					score += Nijk * (Math.log(Nijk) - Math.log(Nij));
				}
			} while (c.nextChild());
		} while (c.nextParents());

		return score;
	}

	@Override
	public double evaluate(Observations observations, List<Integer> parentNodesPast, int childNode) {
		return evaluate(observations, parentNodesPast, null, childNode);
	}

	@Override
	public double evaluate(Observations observations, List<Integer> parentNodesPast, Integer parentNodePresent,
			int childNode) {
		return evaluate(observations, -1, parentNodesPast, parentNodePresent, childNode);
	}

}
