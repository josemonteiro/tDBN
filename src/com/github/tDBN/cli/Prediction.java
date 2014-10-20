package com.github.tDBN.cli;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.tDBN.dbn.CrossValidation;
import com.github.tDBN.dbn.LLScoringFunction;
import com.github.tDBN.dbn.MDLScoringFunction;
import com.github.tDBN.dbn.Observations;
import com.github.tDBN.dbn.ScoringFunction;
import com.github.tDBN.utils.Utils;

public class Prediction {

	@SuppressWarnings({ "static-access" })
	public static void main(String[] args) {

		// create Options object
		Options options = new Options();

		Option observationsFile = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("Input CSV file to be used for network learning.")
				.withLongOpt("inputObservationsFile").create("i");

		Option passiveFile = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("Input CSV file to be used for network assessment.").withLongOpt("inputPassiveFile")
				.create("j");

		Option numParents = OptionBuilder.withArgName("int").hasArg().isRequired()
				.withDescription("Maximum number of parents from preceding time-slice(s).").withLongOpt("numParents")
				.create("p");

		Option markovLag = OptionBuilder
				.withArgName("int")
				.hasArg()
				.isRequired()
				.withDescription(
						"Maximum Markov lag to be considered, which is the longest distance between connected time-slices.")
				.withLongOpt("markovLag").create("m");

		Option numFolds = OptionBuilder.withArgName("int").hasArg()
				.withDescription("Number of fold for cross-validation.").withLongOpt("numFolds").create("k");

		Option forecastAttributes = OptionBuilder.withArgName("int,...").hasArgs().isRequired()
				.withDescription("Class attribute used for stratifying data.").withLongOpt("classAttribute")
				.create("f");

		Option classAttribute = OptionBuilder.withArgName("int").hasArg()
				.withDescription("Attributes that are forecast for the following time-slice.")
				.withLongOpt("forecastAttributes").create("c");

		Option scoringFunction = OptionBuilder.hasArg()
				.withDescription("Scoring function to be used, either MDL or LL. LL is used by default.")
				.withLongOpt("scoringFunction").create("s");

		options.addOption(observationsFile);
		options.addOption(passiveFile);
		options.addOption(numParents);
		options.addOption(markovLag);
		options.addOption(numFolds);
		options.addOption(classAttribute);
		options.addOption(forecastAttributes);
		options.addOption(scoringFunction);

		CommandLineParser parser = new GnuParser();
		try {

			CommandLine cmd = parser.parse(options, args);

			int m = Integer.parseInt(cmd.getOptionValue("m"));
			int p = Integer.parseInt(cmd.getOptionValue("p"));
			int kFolds = Integer.parseInt(cmd.getOptionValue("k", "10"));

			Integer cAttribute = cmd.hasOption("c") ? Integer.parseInt(cmd.getOptionValue("c")) : null;

			List<Integer> fAttributes = new ArrayList<Integer>();
			for (String attribute : cmd.getOptionValues("f"))
				fAttributes.add(Integer.valueOf(attribute));

			String s = cmd.getOptionValue("s", "ll");

			ScoringFunction sf = s.equalsIgnoreCase("ll") ? new LLScoringFunction() : new MDLScoringFunction();

			System.out.println("m = " + m + ", p = " + p + ", k = " + kFolds + ", c = " + cAttribute + ", f = "
					+ fAttributes + ", s = " + s);

			String fileName = cmd.getOptionValue("i");
			String outFileName = fileName.replace(".csv", "");
			outFileName = outFileName + "-" + s + "-m" + m + "-p" + p;

			Observations o = new Observations(fileName, cmd.getOptionValue("j"), m);

			CrossValidation cv = new CrossValidation(o, kFolds, cAttribute);
			String result = cv.evaluate(p, sf, outFileName, fAttributes, true);
			try {
				Utils.writeToFile(outFileName + ".txt", result);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Prediction", options);
		}

	}

}
