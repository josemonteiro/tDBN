package com.github.tDBN.dbn;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.github.tDBN.utils.Utils;

public class Observations {

	/**
	 * Three-dimensional matrix of coded observation data which will be used for
	 * learning a dynamic Bayesian network.
	 * <ul>
	 * <li>the 1st index refers to the transition {t - markovLag + 1, ...
	 * ,t}->t+1;
	 * <li>the 2nd index refers to the the subject (set of observed attributes);
	 * <li>the 3rd index refers to the attribute and lies within the range [0,
	 * (1 + markovLag)*n[, where [0, markovLag*n[ refers to attributes in the
	 * past andand [markovLag*n, (1 + markovLag)*n[ refers to attributes in time
	 * t+1.
	 * </ul>
	 */
	private int[][][] usefulObservations;

	/**
	 * Three-dimensional matrix of observation data that will be present in the
	 * output, but not used for network learning.
	 * <ul>
	 * <li>the 1st index refers to the time slice t;
	 * <li>the 2nd index refers to the the subject (set of observed attributes);
	 * <li>the 3rd index refers to the (not for use) attribute.
	 * </ul>
	 */
	private String[][][] passiveObservations = null;

	// note the difference between transition and time slice in the 1st index

	/**
	 * Indicates, for each subject, what observations are present. Subject ID is
	 * the key, a boolean array of size equal to the number of transitions is
	 * the value.
	 */
	private Map<String, boolean[]> subjectIsPresent;

	/**
	 * Each column of the useful observation data refers to an attribute.
	 */
	private List<Attribute> attributes;

	/**
	 * Number of subjects per transition. Only those who have complete data for
	 * a transition are stored.
	 */
	private int[] numSubjects;

	/**
	 * File that contains observations that will be converted to attributes and
	 * from which one can learn a DBN.
	 */
	private String usefulObservationsFileName;

	/**
	 * File that contains observations that will be included unchanged in the
	 * output. These are ignored when learning a DBN.
	 */
	private String passiveObservationsFileName;

	/**
	 * Header line of input useful observations CSV file.
	 */
	private String[] usefulObservationsHeader;

	/**
	 * Header line of input passive observations CSV file.
	 */
	private String[] passiveObservationsHeader = new String[0];

	/**
	 * Order of the Markov process, which is the number of previous time slices
	 * that influence the values in the following slice. Default is first-order
	 * Markov.
	 */
	private int markovLag = 1;

	/**
	 * Default constructor when reading observations from a file.
	 * 
	 * @see #Observations(String usefulObsFileName, String passiveObsFileName)
	 */
	public Observations(String usefulObsFileName) {
		this(usefulObsFileName, null);
	}

	public Observations(String usefulObsFileName, int markovLag) {
		this(usefulObsFileName, null, markovLag);
	}

	/**
	 * Default constructor when reading observations from a file.
	 * <p>
	 * Input files format is be the following:
	 * <ul>
	 * <li>First row is the header
	 * <li>Each header entry, except the first, is in the form
	 * "attributeName__t", where t is the time slice
	 * <li>First column is the subject ID
	 * <li>One subject per line
	 * <li>No incomplete observations, a subject can only miss entire time
	 * slices.
	 * </ul>
	 * Input file example: <br>
	 * <code>subject_id,"resp__1","age__1","resp__2","age__2","resp__3","age__3"<br>
	 * 121013,0,65.0,0,67.0,0,67.0<br>
	 * 121113,0,24.0,0,29.0,0,29.0<br>
	 * 121114,0,9.0,0,7.0,0,0,7.0<br></code>
	 * 
	 * @param usefulObsFileName
	 *            File which contains observations that will be converted to
	 *            attributes and from which a DBN is learnt.
	 * @param passiveObsFileName
	 *            File which contains observations that will be included
	 *            unchanged in the output. These are ignored when learning a
	 *            DBN.
	 */
	public Observations(String usefulObsFileName, String passiveObsFileName, Integer markovLag) {
		this.usefulObservationsFileName = usefulObsFileName;
		this.passiveObservationsFileName = passiveObsFileName;
		this.markovLag = markovLag != null ? markovLag : 1;
		readFromFiles();
	}

	public Observations(String usefulObsFileName, String passiveObsFileName) {
		this(usefulObsFileName, passiveObsFileName, null);
	}

	/**
	 * This constructor is used when generating observations from a user
	 * specified DBN.
	 * 
	 * @see DynamicBayesNet#generateObservations(int)
	 */
	public Observations(List<Attribute> attributes, int[][][] observationsMatrix) {
		this.attributes = attributes;
		this.usefulObservations = observationsMatrix;
		numSubjects = new int[observationsMatrix.length];

		// assume constant number of observations per transition
		int totalNumSubjects = observationsMatrix[0].length;
		Arrays.fill(numSubjects, totalNumSubjects);

		// generate header
		int n = numAttributes();
		this.usefulObservationsHeader = new String[n];
		for (int i = 0; i < n; i++)
			usefulObservationsHeader[i] = "attribute" + i;

		// assume same subjects over all transitions
		this.subjectIsPresent = new LinkedHashMap<String, boolean[]>((int) Math.ceil(totalNumSubjects / 0.75));
		boolean[] allTrue = new boolean[numTransitions()];
		Arrays.fill(allTrue, true);
		for (int i = 0; i < totalNumSubjects; i++)
			subjectIsPresent.put("" + i, allTrue);
	}

	/**
	 * This constructor is used when forecasting from existing observations.
	 * 
	 * @see DynamicBayesNet#forecast(Observations)
	 */
	public Observations(Observations originalObservations, int[][][] newObservationsMatrix) {
		this.attributes = originalObservations.attributes;
		this.markovLag = originalObservations.markovLag;
		this.passiveObservations = originalObservations.passiveObservations;
		this.passiveObservationsHeader = originalObservations.passiveObservationsHeader;
		this.passiveObservationsFileName = originalObservations.passiveObservationsFileName;
		this.subjectIsPresent = originalObservations.subjectIsPresent;
		this.usefulObservations = newObservationsMatrix;
		this.usefulObservationsHeader = originalObservations.usefulObservationsHeader;
		this.usefulObservationsFileName = originalObservations.usefulObservationsFileName;

		this.numSubjects = new int[usefulObservations.length];

		// assume constant number of observations per transition
		Arrays.fill(numSubjects, usefulObservations[0].length);
	}

	/**
	 * Reads the second and last column of the header, parses the integer time
	 * value and returns the difference between the two, plus one. If parsing is
	 * not possible, exits. Also performs error checking on the number of
	 * columns.
	 * 
	 * @return the number of time slices in input file
	 */
	private static int parseNumTimeSlices(String[] header) {

		int timeFirstColumn = 0, timeLastColumn = 0;

		try {
			// get first and last column time identifier
			timeFirstColumn = Integer.parseInt(header[1].split("__")[1]);
			timeLastColumn = Integer.parseInt(header[header.length - 1].split("__")[1]);

		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(Arrays.deepToString(header));
			System.err.println("Input file header does not comply to the 'attribute__t' format.");
			System.exit(1);
		} catch (NumberFormatException e) {
			System.err.println(Arrays.deepToString(header));
			System.err.println("Input file header does not comply to the 'attribute__t' format.");
			System.exit(1);
		}

		int numTimeSlices = timeLastColumn - timeFirstColumn + 1;

		// the number of columns per time slice must be constant
		// header contains an extra column with subject id
		if ((header.length - 1) % numTimeSlices != 0) {
			System.err.println(Arrays.deepToString(header));
			System.err.println("Input file header does not have a number of columns"
					+ " compatible with the number of time slices.");
			System.err.println("Header length: " + header.length);
			System.err.println("Number of time slices: " + numTimeSlices);
			System.exit(1);
		}

		return numTimeSlices;
	}

	private static int countMissingValues(String[] dataLine) {

		int missing = 0;

		for (String value : dataLine)
			if (value.length() == 0 || value.equals("?"))
				missing++;

		return missing;
	}

	/**
	 * Checks for errors in an array of observed values, in order to decide if
	 * they will be stored in the observations matrix. If all the values are
	 * missing, returns false. If there are some missing values, exits. If no
	 * values are missing, returns true.
	 */
	private boolean observationIsOk(String[] observation) {

		int missingValues = countMissingValues(observation);
		int n = numAttributes();

		if (missingValues == n) {
			// missing observation (all values missing), skip
			return false;
		}

		if (missingValues > 0) {
			// some missing values, can't work like that
			System.err.println(Arrays.deepToString(observation));
			System.err.println("Observation contains missing values.");
			System.exit(1);
		}

		return true;
	}

	private void readFromFiles() {

		try {

			// open and parse the useful observations csv file
			CSVReader reader = new CSVReader(new FileReader(usefulObservationsFileName));
			List<String[]> lines = reader.readAll();
			reader.close();

			ListIterator<String[]> li = lines.listIterator();

			// get first line
			String[] header = li.next();

			int numTimeSlices = parseNumTimeSlices(header);
			int numTransitions = numTimeSlices - markovLag;

			int numAttributes = (header.length - 1) / numTimeSlices;
			attributes = new ArrayList<Attribute>(numAttributes);

			usefulObservationsHeader = processHeader(header, numAttributes);

			// allocate observations matrix
			int totalNumSubjects = lines.size();
			usefulObservations = new int[numTransitions][totalNumSubjects][(markovLag + 1) * numAttributes];
			numSubjects = new int[numTransitions];
			subjectIsPresent = new LinkedHashMap<String, boolean[]>((int) Math.ceil(totalNumSubjects / 0.75));

			String[] dataLine = li.next();

			// fill attributes from first observation (get their type)
			// it must not have missing values
			String[] firstObservation = Arrays.copyOf(dataLine, numAttributes);
			if (countMissingValues(firstObservation) > 0) {
				System.err.println(firstObservation);
				System.err.println("First observation contains missing values.");
				System.exit(1);
			}
			int i = 0;
			for (String value : firstObservation) {
				Attribute attribute;
				// numeric attribute
				if (Utils.isNumeric(value))
					attribute = new NumericAttribute();
				// nominal attribute
				else
					attribute = new NominalAttribute();
				attribute.setName(usefulObservationsHeader[i++]);
				attributes.add(attribute);
			}

			// rewind one line
			li.previous();

			// auxiliary variable
			String[][] observations = new String[markovLag + 1][numAttributes];

			while (li.hasNext()) {

				dataLine = li.next();

				// check for line sanity
				if (dataLine.length != numTimeSlices * numAttributes + 1) {
					System.err.println(Arrays.deepToString(dataLine));
					System.err
							.println("Observations file: input data line does not have the correct number of columns.");
					System.err.println("Line length: " + dataLine.length);
					System.err.println("Number of time slices: " + numTimeSlices);
					System.err.println("Number of attributes: " + numAttributes);
					System.exit(1);
				}

				// record subject id
				String subject = dataLine[0];
				subjectIsPresent.put(subject, new boolean[numTransitions]);

				for (int t = 0; t < numTransitions; t++) {

					boolean observationsOk = true;

					// obtain and check observations for each slice
					for (int ts = 0; ts < markovLag + 1; ts++) {
						observations[ts] = Arrays.copyOfRange(dataLine, 1 + (t + ts) * numAttributes, 1 + (t + ts + 1)
								* numAttributes);
						if (!observationIsOk(observations[ts])) {
							observationsOk = false;
							break;
						}
					}

					if (observationsOk) {

						// observations are sane, store them
						subjectIsPresent.get(subject)[t] = true;
						String[] transition = Arrays.copyOfRange(dataLine, 1 + t * numAttributes, 1
								+ (t + markovLag + 1) * numAttributes);
						for (int j = 0; j < (markovLag + 1) * numAttributes; j++) {
							String value = transition[j];
							int attributeId = j % numAttributes;
							Attribute attribute = attributes.get(attributeId);
							attribute.add(value);
							usefulObservations[t][numSubjects[t]][j] = attribute.getIndex(value);
						}
						numSubjects[t]++;

					} else {
						// if one of the observations has missing values,
						// they are not used in this transition
						subjectIsPresent.get(subject)[t] = false;
					}

				}
			}

		} catch (IOException e) {
			System.err.println("File " + usefulObservationsFileName + " could not be opened.");
			e.printStackTrace();
			System.exit(1);
		}

		if (passiveObservationsFileName != null) {

			try {
				// open and parse the passive observations csv file
				CSVReader reader = new CSVReader(new FileReader(passiveObservationsFileName));
				List<String[]> lines = reader.readAll();
				reader.close();

				ListIterator<String[]> li = lines.listIterator();

				// get first line
				String[] header = li.next();

				int numTimeSlices = numTransitions() + markovLag;
				int totalNumSubjects = subjectIsPresent.size();
				int numPassiveAttributes = (header.length - 1) / numTimeSlices;

				passiveObservationsHeader = processHeader(header, numPassiveAttributes);

				// allocate observations matrix
				passiveObservations = new String[numTimeSlices][totalNumSubjects][numPassiveAttributes];

				int s = 0;
				while (li.hasNext()) {
					String[] dataLine = li.next();
					if (dataLine.length - 1 != numTimeSlices * numPassiveAttributes) {
						System.err.println(Arrays.deepToString(dataLine));
						System.err
								.println("Passive observations file: input data line does not have the correct number of columns.");
						System.err.println("Line length: " + dataLine.length);
						System.err.println("Number of time slices: " + numTimeSlices);
						System.err.println("Number of attributes: " + numPassiveAttributes);
						System.exit(1);
					}

					String subject = dataLine[0];
					if (subjectIsPresent.containsKey(subject)) {
						for (int t = 0; t < numTimeSlices; t++) {
							passiveObservations[t][s] = Arrays.copyOfRange(dataLine, 1 + t * numPassiveAttributes, 1
									+ (t + 1) * numPassiveAttributes);
						}
						s++;
					} else
						System.out.println("Skipping subject " + subject + " on passive observations file.");
				}
			} catch (IOException e) {
				System.err.println("File " + passiveObservationsFileName + " could not be opened.");
				e.printStackTrace();
				System.exit(1);
			}
		}

	}

	/**
	 * Gets the name of the attributes from an input header line and the number
	 * of attributes.
	 */
	private String[] processHeader(String[] header, int numAttributes) {
		String[] newHeader = new String[numAttributes];
		String stripFirstHeader[] = Arrays.copyOfRange(header, 1, numAttributes + 1);
		int i = 0;
		for (String column : stripFirstHeader) {
			String[] columnParts = column.split("__");
			newHeader[i++] = columnParts[0];
		}
		return newHeader;
	}

	public int numTransitions() {
		return usefulObservations.length;
	}

	public int numObservations(int transition) {

		// stationary process
		if (transition < 0) {
			int numObs = 0;
			int T = numTransitions();
			for (int t = 0; t < T; t++)
				numObs += numSubjects[t];
			return numObs;
		}

		// time-varying process
		return numSubjects[transition];
	}

	public int numAttributes() {
		return attributes.size();
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * Returns a representation of the first observations (#markovLag time
	 * slices) of all subjects.
	 */
	public List<int[]> getFirst() {
		int numSubjects = this.numSubjects[0];
		List<int[]> initialObservations = new ArrayList<int[]>(numSubjects);
		for (int s = 0; s < numSubjects; s++)
			initialObservations.add(Arrays.copyOfRange(usefulObservations[0][s], 0, markovLag * numAttributes()));
		return initialObservations;
	}

	/**
	 * Given a network configuration (parents and child values), counts all
	 * observations in some transition that are compatible with it. If
	 * transition is negative, counts matches in all transitions.
	 */
	public int count(LocalConfiguration c, int transition) {

		// stationary process
		if (transition < 0) {
			int allMatches = 0;
			int T = numTransitions();
			for (int t = 0; t < T; t++)
				allMatches += count(c, t);
			return allMatches;
		}

		// time-varying process
		int matches = 0;
		int N = numObservations(transition);
		for (int i = 0; i < N; i++)
			if (c.matches(usefulObservations[transition][i]))
				matches++;
		return matches;
	}

	public void writeToFile() {
		String outFileName = this.usefulObservationsFileName.replace(".csv", "-out.csv");

		// test for file name without extension
		if (outFileName.equals(this.usefulObservationsFileName))
			outFileName = this.usefulObservationsFileName + "-out";

		writeToFile(outFileName);
	}

	public void writeToFile(String outFileName) {

		CSVWriter writer;

		try {

			writer = new CSVWriter(new FileWriter(outFileName));

			int numTransitions = numTransitions();
			int numTimeSlices = numTransitions + 1;
			int numAttributes = numAttributes();
			int numPassiveAttributes = numPassiveAttributes();
			int totalNumAttributes = numAttributes + numPassiveAttributes;
			int numSubjects = this.numSubjects[0];

			boolean thereArePassiveObservations = passiveObservations != null ? true : false;

			int interSliceSpace = 5;

			// compose header line
			List<String> headerEntries = new ArrayList<String>(totalNumAttributes * numTimeSlices + 2 + interSliceSpace);
			headerEntries.add("subject_id");
			for (int t = 0; t < numTimeSlices; t++) {
				for (String columnName : usefulObservationsHeader) {
					headerEntries.add(columnName + "__" + t);
				}
				if (thereArePassiveObservations) {
					// separator between useful (predicted) and passive
					// (unchanged)
					// observations
					headerEntries.add("");
					for (String columnName : passiveObservationsHeader) {
						headerEntries.add(columnName + "__" + t);
					}
					// separator between time slices;
					for (int i = interSliceSpace; i-- > 0;)
						headerEntries.add("");
				}
			}

			// write header line to file
			writer.writeNext(headerEntries.toArray(new String[0]));

			// iterator over subject ids
			Iterator<String> subjectIterator = subjectIsPresent.keySet().iterator();

			int passiveSubject = -1;
			for (int s = 0; s < numSubjects; s++) {

				List<String> subjectEntries = new ArrayList<String>(totalNumAttributes * numTimeSlices + 2
						+ interSliceSpace);

				// add subject id
				while (subjectIterator.hasNext()) {
					String subject = subjectIterator.next();
					passiveSubject++;
					if (subjectIsPresent.get(subject)[0]) {
						subjectEntries.add(subject);
						break;
					}
				}

				// add observations from all except the last time slice
				for (int t = 0; t < numTransitions; t++) {
					for (int i = 0; i < numAttributes; i++) {
						subjectEntries.add(attributes.get(i).get(usefulObservations[t][s][i]));
					}

					if (thereArePassiveObservations) {
						subjectEntries.add("");
						for (int i = 0; i < numPassiveAttributes; i++) {
							subjectEntries.add(passiveObservations[t][passiveSubject][i]);
						}
						for (int i = interSliceSpace; i-- > 0;)
							subjectEntries.add("");
					}
				}

				// add observations from the last time slice
				for (int i = numAttributes; i < 2 * numAttributes; i++) {
					subjectEntries.add(attributes.get(i % numAttributes).get(
							usefulObservations[numTransitions - 1][s][i]));
				}

				if (thereArePassiveObservations) {
					subjectEntries.add("");
					for (int i = 0; i < numPassiveAttributes; i++) {
						subjectEntries.add(passiveObservations[numTimeSlices - 1][passiveSubject][i]);
					}
					for (int i = interSliceSpace; i-- > 0;)
						subjectEntries.add("");
				}
				// write subject line to file
				writer.writeNext(subjectEntries.toArray(new String[0]));

			}

			writer.close();

		} catch (IOException e) {
			System.err.println("Could not write to " + outFileName + ".");
			e.printStackTrace();
			System.exit(1);
		}

	}

	private int numPassiveAttributes() {
		return passiveObservations != null ? passiveObservations[0][0].length : 0;
	}

	public int getMarkovLag() {
		return markovLag;
	}

	public String toTimeSeriesHorizontal() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int numTransitions = numTransitions();
		int numAttributes = numAttributes();

		sb.append("Attribute_ID" + "\t");
		for (int t = 0; t < numTransitions; t++) {
			sb.append("OBS" + t + "\t");
		}
		sb.append("OBS" + numTransitions + ls);
		for (int j = 0; j < numAttributes; j++) {
			sb.append("A" + j + "\t");
			for (int t = 0; t < numTransitions; t++) {
				sb.append(usefulObservations[t][0][j] + "\t");
			}
			sb.append(usefulObservations[numTransitions - 1][0][j + numAttributes] + ls);

		}
		sb.append(ls);
		return sb.toString();
	}

	public String toTimeSeriesVertical() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int numTransitions = numTransitions();
		int numAttributes = numAttributes();

		for (int t = 0; t < numTransitions; t++) {
			for (int j = 0; j < numAttributes; j++)
				sb.append(usefulObservations[t][0][j] + "\t");
			sb.append(ls);
		}
		for (int j = 0; j < numAttributes; j++)
			sb.append(usefulObservations[numTransitions - 1][0][j + numAttributes] + "\t");
		sb.append(ls);

		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String ls = System.getProperty("line.separator");
		int numTransitions = numTransitions();
		int numAttributes = numAttributes();
		// int numColumns = numAttributes*2;

		sb.append("Input file: " + usefulObservationsFileName + ls + ls);

		sb.append("Number of transitions: " + numTransitions + ls);
		sb.append("Number of attributes: " + numAttributes + ls);

		sb.append(ls);

		for (int t = 0; t < numTransitions; t++) {

			sb.append("--- Transition " + t + " ---" + ls);
			int numObservations = numObservations(t);

			sb.append(numObservations + " observations." + ls);

			// sb.append("Observations matrix:"+ls);
			// for (int i=0; i<numObservations; i++) {
			// for(int j=0; j<numColumns; j++) {
			// int attributeId = j%numAttributes;
			// sb.append(attributes.get(attributeId).get(observationsMatrix[t][i][j])+" ");
			// }
			// sb.append(ls);
			// }
			//
			// sb.append(ls);
			//
			// sb.append("Coded observations matrix:"+ls);
			// for (int i=0; i<numObservations; i++) {
			// for(int j=0; j<numColumns; j++) {
			// sb.append(observationsMatrix[t][i][j]+" ");
			// }
			// sb.append(ls);
			// }
			// sb.append(ls);
		}

		sb.append(ls);

		sb.append("Attributes:" + ls);
		for (int i = 0; i < numAttributes; i++) {
			sb.append(attributes.get(i) + ls);
		}

		return sb.toString();

	}

	public static void main(String[] args) {

		Observations o1 = new Observations(args[0], args[1], 2);
		System.out.println(o1);

		boolean statProc = true;
		int numParents = 2;

		System.out.println("initializing scores");
		Scores s1 = new Scores(o1, numParents, statProc);
		System.out.println("evaluating scores");
		s1.evaluate(new MDLScoringFunction());
		System.out.println(s1);

		System.out.println("converting to DBN");
		DynamicBayesNet dbn1 = s1.toDBN();

		try {
			Utils.writeToFile("/home/zlm/dot.dot", dbn1.toDot(false));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("learning DBN parameters");
		dbn1.learnParameters(o1, statProc);

		System.out.println("forecasting observations");
		dbn1.forecast(o1, 5, statProc).writeToFile();

	}

}
