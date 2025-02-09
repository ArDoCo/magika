package edu.kit.kastel.mcse.ardoco;

import ai.onnxruntime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FileTypePredictor {
    private static final Logger logger = Logger.getLogger(FileTypePredictor.class.getName());
    private static final Path defaultPath = Paths.get("src/main/resources/model.onnx");

    private final Path modelPath;
    private final Configuration configuration;
    private final List<String> labels;

    public FileTypePredictor(Path modelPath, Path configurationPath) {
        this.modelPath = modelPath;
        configuration = new Configuration(configurationPath.toString());
        labels = configuration.getTargetLabels();
    }

    public FileTypePredictor(Path modelPath) {
        this.modelPath = modelPath;
        configuration = new Configuration();
        labels = configuration.getTargetLabels();
    }

    public FileTypePredictor() {
        this(defaultPath);
    }

    public Prediction predictFileType(Path inputFilePath) {
        if (!Files.exists(modelPath)) {
            logger.warning("Model path does not exist: " + modelPath.toAbsolutePath());
        }
        if (!Files.exists(inputFilePath)) {
            logger.warning("Input file path does not exist: " + modelPath.toAbsolutePath());
        }


        var config = new Configuration();
        var fullInput = readFileToBuffer(inputFilePath.toFile());
        int[][] inputBuffer = createInputBuffer(fullInput,config);

        var env = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
             var session = env.createSession(modelPath.toAbsolutePath().toString(),opts)){

            String inputName = session.getInputNames().iterator().next();
            var tensor = OnnxTensor.createTensor(env, inputBuffer);
            var result = session.run(Collections.singletonMap(inputName,tensor));
            float[][] outputProbs = (float[][]) result.get(0).getValue();
            int labelIndex = predict(outputProbs[0]);

            return new Prediction(labels.get(labelIndex), outputProbs[0][labelIndex]);
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Path,Prediction> predictFileTypes(Collection<Path> inputFiles) {
        Map<Path, Prediction> predictions = new HashMap<>();
        for (var inputFile : inputFiles) {
            if (!Files.isDirectory(inputFile)) {
                var prediction = predictFileType(inputFile);
                predictions.put(inputFile,prediction);
            }
        }
        return predictions;
    }

    public Map<Path,Prediction> predictFileTypesFromFolderRecursively(Path inputFolder) {
        checkFolder(inputFolder);

        Set<Path> files = findAllFilesRecursively(inputFolder);
        return predictFileTypes(files);
    }

    private static void checkFolder(Path inputFolder) {
        if (!Files.exists(inputFolder)) {
            logger.warning("Provided path does not exist: " + inputFolder.toAbsolutePath());
            throw new IllegalArgumentException();
        }
        if (!Files.isDirectory(inputFolder)) {
            logger.warning("Provided path is not a folder: " + inputFolder.toAbsolutePath());
            throw new IllegalArgumentException();
        }
    }

    public Map<Path,Prediction> predictFileTypesFromFolder(Path inputFolder) {
        checkFolder(inputFolder);

        Set<Path> files = findAllFilesRecursively(inputFolder, 1);
        return predictFileTypes(files);
    }

    private static Set<Path> findAllFilesRecursively(Path folder) {
        return findAllFilesRecursively(folder, 999);
    }

    private static Set<Path> findAllFilesRecursively(Path folder, int depth) {
        try {
            return Files.find(folder, depth, (p, bfa) -> bfa.isRegularFile()).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[][] createInputBuffer(byte[] fullInput, Configuration config) {
        int beginningSize = config.getBeginningSize();
        int midSize = config.getMidSize();
        int endSize = config.getEndSize();
        var bufferSize = beginningSize + midSize + endSize;
        int paddingToken = config.getPaddingToken();

        int[] beginningArray = new int[beginningSize];
        for (int i = 0; i < beginningArray.length; i++) {
            if (i < fullInput.length) {
                beginningArray[i] = fullInput[i];
            } else {
                beginningArray[i] = paddingToken;
            }
        }

        int[] midArray = new int[midSize];
        int halfInputSize = Math.round(fullInput.length / 2);
        int startHalf = Math.max(0, halfInputSize - (midSize / 2));
        for (int i = 0; i < midArray.length; i++) {
            int inputIndex = startHalf + i;
            if (i < fullInput.length) {
                midArray[i] = fullInput[inputIndex];
            } else {
                midArray[i] = paddingToken;
            }
        }

        int[] endArray = new int[endSize];
        for (int i = 1; i <= endArray.length; i++) {
            int inputIndex = fullInput.length - i;
            int endArrayIndex = endArray.length - i;
            if (i <= fullInput.length) {
                endArray[endArrayIndex] = fullInput[inputIndex];
            } else {
                endArray[endArrayIndex] = paddingToken;
            }
        }

        int[] inputArray = IntStream.concat(IntStream.concat(Arrays.stream(beginningArray), Arrays.stream(midArray)), Arrays.stream(endArray)).toArray();
        int[][] returnArray = new int[1][bufferSize];
        returnArray[0] = inputArray;
        return returnArray;
    }

    /**
     * Find the maximum probability and return it's index.
     *
     * @param probabilities The probabilities.
     * @return The index of the max.
     */
    public static int predict(float[] probabilities) {
        float maxValue = Float.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxValue) {
                maxValue = probabilities[i];
                index = i;
            }
        }
        return index;
    }

    private static byte[] readFileToBuffer(File file) {
        byte[] byteArray = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            inputStream.read(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteArray;
    }

    private static void printOutputProbs(float[][] outputProbs) {
        logger.info(Arrays.toString(outputProbs[0]));
    }

    private static void logModelInfo(OrtSession session) throws OrtException {
        logger.info("Inputs:");
        for (NodeInfo i : session.getInputInfo().values()) {
            logger.info(i.toString());
        }

        logger.info("Outputs:");
        for (NodeInfo i : session.getOutputInfo().values()) {
            logger.info(i.toString());
        }
    }
}
