package com.github.tDBN.cli;

import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.tDBN.dbn.DynamicBayesNet;
import com.github.tDBN.dbn.LLScoringFunction;
import com.github.tDBN.dbn.MDLScoringFunction;
import com.github.tDBN.dbn.Observations;
import com.github.tDBN.dbn.Scores;
import com.github.tDBN.utils.Utils;

public class LearnFromFile {

	@SuppressWarnings({ "static-access" })
	public static void main(String[] args) {

		// create Options object
		Options options = new Options();

		Option learningFile = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("Input CSV file to be used for network learning.").withLongOpt("inputFile")
				.create("i");

		Option numParents = OptionBuilder.hasArg().isRequired()
				.withDescription("Number of parents from preceding time slice.").withLongOpt("numParents").create("p");

		Option outputNetwork = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("Output file with the network structure. ").withLongOpt("outputFile").create("o");

		Option rootNode = OptionBuilder.withArgName("node").hasArg()
				.withDescription("Root node of the intra-slice tree. By default, root is arbitrary.")
				.withLongOpt("root").create("r");

		Option scoringFunction = OptionBuilder.hasArg()
				.withDescription("Scoring function to be used, either LL or MDL. LL used by default.")
				.withLongOpt("scoringFunction").create("s");

		Option dotFormat = OptionBuilder.withDescription("If specified, outputs network in dot format.")
				.withLongOpt("dotFormat").create("d");

		Option compact = OptionBuilder
				.withDescription(
						"If specified together with -d, outputs network in compact format, ommiting intra-slice edges.")
				.withLongOpt("compact").create("c");

		options.addOption(learningFile);
		options.addOption(numParents);
		options.addOption(outputNetwork);
		options.addOption(rootNode);
		options.addOption(scoringFunction);
		options.addOption(dotFormat);
		options.addOption(compact);

		CommandLineParser parser = new GnuParser();
		try {

			CommandLine cmd = parser.parse(options, args);

			Observations o = new Observations(cmd.getOptionValue("i"));
			Scores s = new Scores(o, Integer.parseInt(cmd.getOptionValue("p")), true);
			if (cmd.hasOption("s") && cmd.getOptionValue("s").equalsIgnoreCase("mdl")) {
				System.out.println("Evaluating network with MDL score.");
				s.evaluate(new MDLScoringFunction());
			} else {
				System.out.println("Evaluating network with LL score.");
				s.evaluate(new LLScoringFunction());
			}

			DynamicBayesNet dbn;

			if (cmd.hasOption("r")) {
				int r = Integer.parseInt(cmd.getOptionValue("r"));
				System.out.println("Root node specified: " + r);
				dbn = s.toDBN(r);
			} else {
				System.out.println("No root specified.");
				dbn = s.toDBN();
			}

			try {
				if (cmd.hasOption("d") && cmd.hasOption("c"))
					Utils.writeToFile(cmd.getOptionValue("o"), dbn.toDotSimple(true));
				else if (cmd.hasOption("d") && !cmd.hasOption("c"))
					Utils.writeToFile(cmd.getOptionValue("o"), dbn.toDotSimple(false));
				else
					Utils.writeToFile(cmd.getOptionValue("o"), dbn.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("tldbn", options);
		}

	}

}
