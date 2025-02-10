/* Licensed under Apache 2.0 2025. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class FileTypePredictor {
    private static final Logger logger = Logger.getLogger(FileTypePredictor.class.getName());
    private static final Path defaultPath = Paths.get("src/main/resources/model.onnx");

    private final Path modelPath;
    private final Configuration configuration;
    private final List<String> labels;

    /**
     * Create a new predictor that uses the provided model path and configuration path.
     *
     * @param modelPath         the model path
     * @param configurationPath the configuration path
     */
    public FileTypePredictor(Path modelPath, Path configurationPath) {
        this.modelPath = modelPath;
        configuration = new Configuration(configurationPath.toString());
        labels = configuration.getTargetLabels();
    }

    /**
     * Create a new predictor that uses the provided model path.
     *
     * @param modelPath the model path
     */
    public FileTypePredictor(Path modelPath) {
        this.modelPath = modelPath;
        configuration = new Configuration();
        labels = configuration.getTargetLabels();
    }

    /**
     * Create a new predictor that uses the default model path and configuration path.
     */
    public FileTypePredictor() {
        this(defaultPath);
    }

    /**
     * Predict the file type of the provided file.
     *
     * @param inputFilePath the input file
     * @return the predicted file type
     */
    public Prediction predictFileType(Path inputFilePath) {
        if (!Files.exists(modelPath)) {
            logger.warning("Model path does not exist: " + modelPath.toAbsolutePath());
        }
        if (!Files.exists(inputFilePath)) {
            logger.warning("Input file path does not exist: " + modelPath.toAbsolutePath());
        }

        var config = new Configuration();

        File inputFile = inputFilePath.toFile();
        if (inputFile.length() == 0) {
            return new Prediction("empty", 1.f);
        } else if (inputFile.length() <= this.configuration.getMinSize()) {
            return new Prediction("txt", 1.f);
        }

        int[][] inputBuffer = readFileToInputBuffer(inputFile, config);
        return getPrediction(inputBuffer);
    }

    private Prediction getPrediction(int[][] inputBuffer) {
        var env = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions(); var session = env.createSession(modelPath.toAbsolutePath().toString(), opts)) {

            String inputName = session.getInputNames().iterator().next();
            var tensor = OnnxTensor.createTensor(env, inputBuffer);
            var result = session.run(Collections.singletonMap(inputName, tensor));
            float[][] outputProbabilities = (float[][]) result.get(0).getValue();
            int labelIndex = predict(outputProbabilities[0]);

            return new Prediction(labels.get(labelIndex), outputProbabilities[0][labelIndex]);
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Predict the file type of the provided files.
     *
     * @param inputFiles the input files
     * @return a map that consists of the path of the file and the corresponding predicted file type
     */
    public Map<Path, Prediction> predictFileTypes(Collection<Path> inputFiles) {
        Map<Path, Prediction> predictions = new HashMap<>();
        for (var inputFile : inputFiles) {
            if (!Files.isDirectory(inputFile)) {
                var prediction = predictFileType(inputFile);
                predictions.put(inputFile, prediction);
            }
        }
        return predictions;
    }

    /**
     * Predict the file type of the files that are located in the provided folder, recursively.
     *
     * @param inputFolder the input folder
     * @return a map that consists of the path of the file and the corresponding predicted file type
     */
    public Map<Path, Prediction> predictFileTypesFromFolderRecursively(Path inputFolder) {
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

    /**
     * Predict the file type of the files that are located in the provided folder, non-recursively.
     *
     * @param inputFolder the input folder
     * @return a map that consists of the path of the file and the corresponding predicted file type
     */
    public Map<Path, Prediction> predictFileTypesFromFolder(Path inputFolder) {
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

    /**
     * Find the maximum probability and return it's index.
     *
     * @param probabilities The probabilities.
     * @return The index of the max.
     */
    private static int predict(float[] probabilities) {
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

    private static int[][] readFileToInputBuffer(File file, Configuration config) {
        long fileLength = file.length();

        int beginningSize = config.getBeginningSize();
        int midSize = config.getMidSize();
        int endSize = config.getEndSize();
        var bufferSize = beginningSize + midSize + endSize;
        byte paddingToken = (byte) config.getPaddingToken();

        byte[] beginningArray = new byte[beginningSize];
        Arrays.fill(beginningArray, paddingToken);

        byte[] midArray = new byte[midSize];
        Arrays.fill(midArray, paddingToken);

        int endByteArraySize = Math.min((int) fileLength, endSize);
        byte[] endArray = new byte[endByteArraySize];
        Arrays.fill(endArray, paddingToken);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            raf.read(beginningArray);

            int offset = 0;
            if (midSize > 0) {
                int halfInputSize = Math.round((float) fileLength / 2);
                offset = Math.max(0, halfInputSize - (midSize / 2));
                raf.seek(offset);
                raf.read(midArray);
            }

            offset = (int) Math.max(0, fileLength - endSize);
            raf.seek(offset);
            raf.read(endArray);
            if (endSize > endByteArraySize) {
                byte[] tmp = new byte[endSize];
                Arrays.fill(tmp, paddingToken);
                for (int i = 1; i <= endArray.length; i++) {
                    int indexEnd = endArray.length - i;
                    int indexTmp = tmp.length - i;
                    tmp[indexTmp] = endArray[indexEnd];
                }
                endArray = tmp;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] inputArray = new int[beginningSize + midSize + endSize];
        int maxSize = Math.max(Math.max(beginningSize, midSize), endSize);
        for (int i = 0; i < maxSize; i++) {
            if (i < beginningSize) {
                inputArray[i] = beginningArray[i];
            }
            if (i < midSize) {
                inputArray[i + beginningSize] = midArray[i];
            }
            if (i < endSize) {
                inputArray[i + beginningSize + midSize] = endArray[i];
            }
        }

        int[][] returnArray = new int[1][bufferSize];
        returnArray[0] = inputArray;
        return returnArray;
    }
}
