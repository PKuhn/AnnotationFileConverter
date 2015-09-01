import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileParser {

    private final String PARSING_OPTIONS = "normalizeParentheses=false, asciiQuotes=true, " +
            "latexQuotes=false, ptb3Dashes=false, normalizeOtherBrackets=false, ptb3Ellipsis=false, unicodeEllipsis=false," +
            "normalizeFractions = false";

    public static void main(String[] args) {
        // Use Apache Commons CLI to handle command line input.
        CommandLineParser cmdParser = new DefaultParser();

        // Create CommonsCLI Input Options
        Options options = new Options();

        Option inputDirectory = Option.builder("i")
                .required(true)
                .longOpt("input")
                .hasArg()
                .argName("directory")
                .desc("directory that conatins *.txt and *.ann files")
                .build();

        Option outputDirectory = Option.builder("o")
                .required(true)
                .longOpt("output")
                .hasArg()
                .argName("directory")
                .desc("directory where to output the merged *.tsv file")
                .build();

        options.addOption(inputDirectory);
        options.addOption(outputDirectory);

        try {
            // parse the command line arguments
            CommandLine line = cmdParser.parse( options, args );

            if(line.hasOption("i") && line.hasOption("o")){

                FileParser parser = new FileParser();
                List<String> allowedLabels = new ArrayList<>();
                allowedLabels.add("COMP");
                parser.parseAnnotationFilesInDirectory(line.getOptionValue("i"), line.getOptionValue("o"), "merged",allowedLabels);
            }else{
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("FileParser", options);
            }
        }
        catch( ParseException exp ) {
            //print help text
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("FileParser", options);

            // oops, something went wrong => print Reason
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        }
    }

    /**
     * Scans directory for all txt files where an according .ann file is present and converts them in
     * a single TSV file named merged.tsv
     * @param path path of directory where txt and ann files should be searched
     * @param outputPath
     * @param outputFileName
     * @param allowedLabels
     */
    public void parseAnnotationFilesInDirectory(String path, String outputPath, String outputFileName, List<String> allowedLabels) {
        List<String> fileNames = getFileNames(path);
        fileNames.stream().filter(fileName -> fileName.contains(".txt")
                && fileNames.contains(StringUtils.substringBefore(fileName, ".txt") + ".ann")).forEach(fileName -> {
            System.out.println("started creating tsv for: " + fileName);

            try {
                createTSVFile(StringUtils.substringBefore(fileName, ".txt"), path, allowedLabels);
                System.out.println("created tsv for: " + fileName);
            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("There was an error with text: " + fileName + " while searching the start positions of the tokens");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("There was an error with text: " + fileName + " while matching the tokens");
            }

        });
        mergeTSVFiles(path, outputPath, outputFileName);
    }

    /**
     * Merges together all TSV files files in a given directory, produced from single .ann .txt file pairs.
     * These files are separated with a free line according to Standford NER input format
     * @param path path of directory
     */
    public static void mergeTSVFiles(String path, String outputPath, String fileName) {
        FileParser parser = new FileParser();
        List<String> fileNames = parser.getFileNames(path);
        try {
            fileName += ".tsv";
            PrintWriter writer = new PrintWriter(outputPath + File.separator + fileName, "UTF-8");
            fileNames.stream().filter(file -> file.contains(".tsv")).forEach(file -> {
                List<String> lines = readFileToLines(file, path);
                lines.forEach(writer::println);
                writer.println();
            });

            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates TSV File for a single text. Requires according .ann File to be present.
     * @param textName name of text wanted to convert to TSV format
     * @param path path to directory where .txt and .ann file are
     */

    /**
     * Creates TSV File for a single text. Requires according .ann File to be present.
     * @param textName name of text wanted to convert to TSV format
     * @param path path to directory where .txt and .ann file are
     * @param allowedLabels
     */
    public void createTSVFile(String textName, String path, List<String> allowedLabels) {
        FileParser parser = new FileParser();
        List<AnnotationEntity> entities = parser.readInAnnotationFile(textName, path, allowedLabels);
        try {
            List<String> tokens = parser.tokenizeText(textName, path);
            String text = parser.readInText(textName, path);
            tokens = preprocessTokens(tokens);

            List<Integer> startingPositions = parser.findStartingPositionsOfTokens(tokens, text);

            List<String> labels = parser.matchTokens(tokens, entities, startingPositions);
            parser.writeAnnotationsToTSV(tokens, labels, textName, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Scans directory to list all file names present. This is used to find all pairs of files where an
     * .ann and .txt file is present
     * @param path path to directory which should be checked for filenames
     * @return all file names as string containing the file ending as well
     */
    private List<String> getFileNames(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    /**
     * Reads annotation file for a text and generates AnnotationEntities for that file.
     * @param textName name of the text for which the .ann file should be parsed
     * @param path path to directory where name.txt and name.ann are present in this directory
     * @return List of entities in own AnnotationEntity format
     */
    private List<AnnotationEntity> readInAnnotationFile(String textName, String path,
                                                        List<String> allowedLabels) {
        String fileName = textName + ".ann";

        List<String> annotations = readFileToLines(fileName, path);
        List<AnnotationEntity> entities= new ArrayList<>();
        int offset = 0;
        int id = 0;
        for (String annotation : annotations) {
            List<AnnotationEntity> entitiesInLine =  parseAnnotationLine(annotation, id,
                    allowedLabels, offset);
            entities.addAll(entitiesInLine);
            id += entitiesInLine.size();
        }
        entities = sortAnnotations(entities);
        return entities;
    }

    private List<AnnotationEntity> sortAnnotations(List<AnnotationEntity> annotations) {
        Collections.sort(annotations, new Comparator<AnnotationEntity>() {
            public int compare(AnnotationEntity a1, AnnotationEntity a2) {
                return Integer.compare(a1.getStart(), a2.getStart());
            }
        });
        return annotations;
    }

    /**
     * Parses one line in the .ann File into a List of AnnotationEntity. This is needed because it is possible to have
     * annotations that contain multiple Elements.
     * @param line Line in .ann file to be parsed. This line is in format ID \t Label StartPosition EndPosition \t Content
     * @param currentIDCounter current annotation ID needed fro create new Annotation
     * @return List of Annotation Entities which then have only one word as content.
     */
    private List<AnnotationEntity> parseAnnotationLine(String line, int currentIDCounter,
                                                       List<String> allowedLabels, int offset) {

        //line = line.trim().replaceAll(" +", " ");
        String[] annotationParts = line.split("\t");

        String content = annotationParts[2];

        // annotation Information contains [Label StartPosition EndPosition]
        String[] annotationInformation = annotationParts[1].split(" ");
        String label = annotationInformation[0];
        if (!allowedLabels.contains(label)) {
            return new ArrayList<>();
        }

        int startPosition = Integer.parseInt(annotationInformation[1]);

        List<String> tokenizedContent = tokenizeString(new StringReader(content));

        List<String> splitAnnotations = new ArrayList<>();
        for (String token : tokenizedContent) {
            splitAnnotations.addAll(splitAnnotationsByDelimiters(token));
        }

        List<AnnotationEntity> entities = new ArrayList<>();

        int begin = startPosition + offset;
        for (String annotatedWord : splitAnnotations) {
            String ID = "T" + currentIDCounter;

            AnnotationEntity entity = new AnnotationEntity(ID, annotatedWord, begin,
                    begin+annotatedWord.length(), label);

            begin += annotatedWord.length() + 1;
            entities.add(entity);
        }

        return entities;
    }


    private List<String> splitAnnotationsByDelimiters(String content) {
        List<String> contentParts = Arrays.asList(content.split(" ")).stream()
                .map(this::splitTokenByDash).flatMap(Collection::stream)
                .collect(Collectors.toList());


        List<String> splittedCommata =
                contentParts.stream().map(this::splitTokenByComma)
                        .flatMap(Collection::stream).collect(Collectors.toList());

        List<String> splitByAmpersand = splittedCommata.stream().map(this::splitTokenByAmpersand)
                .flatMap(Collection::stream).collect(Collectors.toList());

        List<String> splitByPoint = splitByAmpersand.stream().map(this::splitTokenByPoint)
                .flatMap(Collection::stream).collect(Collectors.toList());

        return splitByPoint;
    }

    /**
     * Takes a string as input and replaces all unicode quotes to the respective ascii
     * representation
     * @param input text in which quotes should be replaced
     * @return text without unicode Quotes
     */
    private String applyAsciiTransformation(String input) {
        String asciiString = input;
        List<Integer> doubleQuotes =
                new ArrayList<>(Arrays.asList(0x201c, 0x201d,0x201e, 0x201f, 0x275d, 0x275e,
                        0x00AB, 0x00BB));
        List<Integer> singleQuotes =
                new ArrayList<>(Arrays.asList(0x2018,0x2019,0x201a,0x201b,0x275b,0x275c));

        for (Integer doubleQuoteHexValue : doubleQuotes) {
            String doubleQuote = String.valueOf(Character.toChars(doubleQuoteHexValue));
            asciiString = asciiString.replaceAll(doubleQuote, "\"");
        }

        for (Integer singleQuoteHexValue : singleQuotes) {
            String singleQuote = String.valueOf(Character.toChars(singleQuoteHexValue));
            asciiString = asciiString.replaceAll(singleQuote, "\'");
        }
        String nonBreakingSpace =String.valueOf(Character.toChars(0x00A0));
        asciiString = asciiString.replaceAll(nonBreakingSpace, " ");

        String tripleDots =String.valueOf(Character.toChars(0x2026));
        asciiString = asciiString.replaceAll(tripleDots, "...");


        return asciiString;
    }
    /**
     * prints matched tokens and labels generated by matchTokens() to an TSV file which can be read by the Standford NER
     * @param tokens list of tokens which the Standford PTB Tokenizer produces for the text which should be parsed to TSV 
     * @param labels List of labels, from the annotated file or "O" for other. This is the output generated of the matchTokens method
     * @param fileName name of TSV file which will be created  
     * @param path output path where the TSV File will be created
     */
    private void writeAnnotationsToTSV(List<String> tokens, List<String> labels, String fileName, String path) {
        try {
            fileName += ".tsv";
            //tokens = postProcessTokens(tokens);
            PrintWriter writer = new PrintWriter(path + File.separator + fileName, "UTF-8");
            for (int i = 0; i < tokens.size(); i++) {
                if (labels.size() <= i) {
                    writer.println(tokens.get(i) + "\t" + "O");
                } else {
                    writer.println(tokens.get(i) + "\t" + labels.get(i));
                }
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Match the positions of the Standford Tokenizer to the positions in the text to compare
     * them with the positions in the .ann files later on.
     * @param tokens List of token Output generated by Standford tokenizer
     * @param text text for which the tokens where generated
     * @return returns the starting position of the tokens as list of Integer
     */
    private List<Integer> findStartingPositionsOfTokens(List<String> tokens, String text) {
        int tokenIndex = 0;
        int textIndex= 0;

        List<Integer> startPositions = new ArrayList<>();

        while (tokenIndex < tokens.size()) {

            String nextToken = tokens.get(tokenIndex);
            int tokenSize = nextToken.length();

            String nextTextToken = text.substring(textIndex, textIndex + tokenSize);
            if (nextTextToken.equals(nextToken)) {
                startPositions.add(textIndex);
                textIndex += tokenSize;
                tokenIndex++;
            } else {
                textIndex++;
            }
        }

        assert (startPositions.size() == tokens.size());
        return startPositions;
    }

    /**
     * helper method that reads in the text and returns it as string with lines separated by " "
     * @param textName name of the text which should be searched
     * @param path directory in which the text should be searched
     * @return text in given text file with lines separated by " "
     */
    private String readInText(String textName, String path) {
        String fileDir = textName + ".txt";
        String text = "";
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(path + File.separator + fileDir), "UTF8"));
            String line;
            while ((line = in.readLine()) != null) {
                text +=  line + " ";
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("invalid encoding");;
        } catch (IOException e) {
            e.printStackTrace();
        }
        text = applyAsciiTransformation(text);
        return text;
    }

    /**
     * Takes input of tokenizer and replaces them with the corresponding text representation
     * @param tokens tokens generated by PTB tokenizer
     * @return list of cleaned tokens
     */

    private List<String> preprocessTokens(List<String> tokens) {
        List<String> processedTokens = new ArrayList<>();
        for (String token : tokens) {
            String asciiToken = applyAsciiTransformation(token);
            processedTokens.addAll(splitAnnotationsByDelimiters(asciiToken));
        }
        return processedTokens;
    }

    /**
     * Method use the generate tokens which have to be labeled to train the Standford NER. This is needed to  
     * partition the text into chunks like expexted from the CRF-Classifier
     * @param textName name of the text which should be tokenized
     * @param path directory in which the text should be searched
     * @return list of tokens produced by PTBTokenizer
     */
    private List<String> tokenizeText(String textName, String path) throws IOException {
        String fileName = textName + ".txt";

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(path+ File.separator +fileName), "UTF8"));

        return tokenizeString(in);
    }

    private List<String> tokenizeString(Reader inputReader) {
        List<String> tokens = new ArrayList<>();

        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(inputReader,
                new CoreLabelTokenFactory(), PARSING_OPTIONS);
        while (ptbt.hasNext()) {
            CoreLabel label = ptbt.next();
            tokens.add(label.value());
        }

        return tokens;
    }


    List<String> splitTokenByDelimiterWithoutDeletion(String input, String delimiter) {
        if (input.equals("Saurenz.BASF")){
            System.out.printf("");
            System.out.println(input.split(delimiter).length);
        }
        if (input.contains(delimiter)) {
            List<String> splittedString = new ArrayList<>();
            if (delimiter.equals(".")) {
                splittedString = new ArrayList<>(Arrays.asList(input.split("\\.")));
            } else {
                splittedString = new ArrayList<>(Arrays.asList(input.split(delimiter)));
            }

            for (int i = 1; i < splittedString.size(); i += 2) {
                // i has to be increased by two because elements are added
                splittedString.add(i, delimiter);
            }
            if (splittedString.size() != 0) {
                return splittedString;
            }
        }
        List<String> result = new ArrayList<>();
        result.add(input);
        return result;
    }
    List<String> splitTokenByComma(String input) {
        return splitTokenByDelimiterWithoutDeletion(input, ",");
    }

    List<String> splitTokenByAmpersand(String input) {
        return splitTokenByDelimiterWithoutDeletion(input, "&");
    }

    List<String> splitTokenByPoint(String input) {
        return splitTokenByDelimiterWithoutDeletion(input, ".");
    }

    List<String> splitTokenByBackslash(String input) {
        return splitTokenByDelimiterWithoutDeletion(input, "\\");
    }

    List<String> splitTokenByDash(String input) {
        if (input.contains("-")) {
            List<String> splittedString = new ArrayList<>(Arrays.asList(input.split("-")));

            for (int i = 1; i < splittedString.size(); i += 2) {
                // i has to be increased by two because elements are added
                splittedString.add(i, "-");
            }
            if (splittedString.size() != 0) {
                return splittedString;
            }
        }
        List<String> result = new ArrayList<>();
        result.add(input);
        return result;
    }

    /**
     * Converts each word of the tokenized text into the representation of the TSV file
     * (word \t label)
     * @param tokens text to convert in token representation
     * @param annotations annotations parsed from according .ann file
     * @param startPositions starting positions of tokens in text
     * @return list of labels in TSV format
     */
    private List<String> matchTokens(List<String> tokens, List<AnnotationEntity> annotations, List<Integer> startPositions) {

        List<String> labels = new ArrayList<>();
        int annotationIndex = 0;
        int textIndex = 0;
        int tokenIndex = 0;
        int matchedTokens = 0;

        while (annotationIndex < annotations.size() && tokenIndex< tokens.size()) {
            AnnotationEntity annotation = annotations.get(annotationIndex);
            String token = tokens.get(tokenIndex);
            annotation= annotations.get(annotationIndex);
            int textPosition = startPositions.get(tokenIndex);

            if (token.equals(annotation.getContent())) {
                labels.add(annotation.getLabel());
                annotationIndex++;
                //System.out.println("matched: " + annotation.getContent());
                matchedTokens++;
            } else if (token.contains(annotation.getContent()) && Math.abs(annotation.getStart()
                    -textIndex) < 10) {
                System.out.println("debug this case");
            }
            else {
                labels.add("O");
            }

            if (textPosition > annotation.getStart() + 10) {
                annotationIndex++;
            }
            tokenIndex++;
        }

        while(tokenIndex< tokens.size()) {
            labels.add("O");
            tokenIndex++;
        }
        //failures += annotations.size() - matchedTokens;
        //System.out.println("matched " + matchedTokens + " from a total of: " + annotations.size());
        return labels;
    }


    /**
     * helper method needed to convert a file to a list of string, where each string represents one line in the file    
     * @param fileName name of the file which should be read in 
     * @param path directory in which the text should be searched
     * @return list if lines of the file as Strings 
     */
    private static List<String> readFileToLines(String fileName, String path) {
        File file = new File(path + File.separator + fileName);
        List<String> lines = new ArrayList<>();


        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private void trainModels() {
        //trainModel("D:\\Job\\AnnotationFileParser\\data\\100Comp.prop", "100comp.ser");
        //trainModel("D:\\Job\\AnnotationFileParser\\data\\200Comp.prop", "200comp.ser");
        //trainModel("D:\\Job\\AnnotationFileParser\\data\\Rand.prop", "100rand.ser");
    }

    private void trainModel(String propPath, String outputName) {
        Properties properties = new Properties();
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream
                    (propPath));
            properties.load(stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CRFClassifier cr = new CRFClassifier(properties);
        cr.train();
        cr.serializeClassifier("D:\\Job\\AnnotationFileParser\\data\\" + outputName);
        CRFClassifier test = new CRFClassifier(properties);
        try {
            test.loadClassifier("D:\\Job\\AnnotationFileParser\\data\\" + outputName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
    /**
     * method needed to read in annotation configurations which can be used to verify if all the labels present in the current
     * annotation file are defined and valid. For now it is enough to just read all entities from the configuration file.
     * Unlike in Brat for the moment the method requires the configuration File to be present in the given location and does not
     * search for it in higher directories     
     * @param path path to the location of the annotation file  
     * @return list of labels of entities defined in the configuration file
     */
    private List<String> parseConfigurationFile(String path) {
        List<String> configuration = readFileToLines("annotation.conf", path);
        List<String> entityLabels = new ArrayList<>();
        for (String line : configuration) {
            if (line.equals("[entities]") || line.equals("")){
                continue;
            }
            else if (line.equals("[relations]")) {
                //for now only read the entities
                break;
            }
            else {
                entityLabels.add(line);
            }
        }
        return entityLabels;
    }
}
