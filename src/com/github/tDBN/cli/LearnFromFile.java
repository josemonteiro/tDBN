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

		Option inputFile = OptionBuilder.withArgName("file").hasArg().isRequired()
				.withDescription("Input CSV file to be used for network learning.").withLongOpt("inputFile")
				.create("i");

		Option numParents = OptionBuilder.hasArg().isRequired()
				.withDescription("Maximum number of parents from preceding time slice(s).").withLongOpt("numParents")
				.create("p");

		Option outputFile = OptionBuilder.withArgName("file").hasArg()
				.withDescription("Writes output to <file>. If not supplied, output is written to terminal.")
				.withLongOpt("outputFile").create("o");

		Option rootNode = OptionBuilder.withArgName("node").hasArg()
				.withDescription("Root node of the intra-slice tree. By default, root is arbitrary.")
				.withLongOpt("root").create("r");

		Option scoringFunction = OptionBuilder.hasArg()
				.withDescription("Scoring function to be used, either MDL or LL. MDL is used by default.")
				.withLongOpt("scoringFunction").create("s");

		Option dotFormat = OptionBuilder
				.withDescription(
						"Outputs network in dot format, allowing direct redirection into Graphviz to visualize the graph.")
				.withLongOpt("dotFormat").create("d");

		Option compact = OptionBuilder
				.withDescription(
						"Outputs network in compact format, omitting intra-slice edges. Only works if specified together with -d and with --markovLag 1.")
				.withLongOpt("compact").create("c");

		Option maxMarkovLag = OptionBuilder
				.hasArg()
				.withDescription(
						"Maximum Markov lag to be considered, which is the longest distance between connected time slices. Default is 1, allowing edges from one preceding slice.")
				.withLongOpt("markovLag").create("l");

		options.addOption(inputFile);
		options.addOption(numParents);
		options.addOption(outputFile);
		options.addOption(rootNode);
		options.addOption(scoringFunction);
		options.addOption(dotFormat);
		options.addOption(compact);
		options.addOption(maxMarkovLag);

		CommandLineParser parser = new GnuParser();
		try {

			CommandLine cmd = parser.parse(options, args);

			boolean verbose = !cmd.hasOption("d");

			int markovLag = 1;
			if (cmd.hasOption("l")) {
				markovLag = Integer.parseInt(cmd.getOptionValue("l"));
				// TODO: check sanity
			}

			Observations o = new Observations(cmd.getOptionValue("i"), markovLag);

			Scores s = new Scores(o, Integer.parseInt(cmd.getOptionValue("p")), true);
			if (cmd.hasOption("s") && cmd.getOptionValue("s").equalsIgnoreCase("ll")) {
				if (verbose)
					System.out.println("Evaluating network with LL score.");
				s.evaluate(new LLScoringFunction());
			} else {
				if (verbose)
					System.out.println("Evaluating network with MDL score.");
				s.evaluate(new MDLScoringFunction());
			}

			DynamicBayesNet dbn;

			if (cmd.hasOption("r")) {
				int r = Integer.parseInt(cmd.getOptionValue("r"));
				if (verbose)
					System.out.println("Root node specified: " + r);
				dbn = s.toDBN(r);
			} else {
				// System.out.println("No root specified.");
				dbn = s.toDBN();
			}

			String output;
			if (cmd.hasOption("d")) {
				if (cmd.hasOption("c") && markovLag == 1)
					output = dbn.toDot(true);
				else
					output = dbn.toDot(false);
			} else
				output = dbn.toString();

			if (cmd.hasOption("o")) {
				try {
					Utils.writeToFile(cmd.getOptionValue("o"), output);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println();
				System.out.println(output);
			}

		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("tDBN", options);
		}

	}
}
