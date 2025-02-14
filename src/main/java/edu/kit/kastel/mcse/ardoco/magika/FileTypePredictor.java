/* Licensed under Apache 2.0 2025. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class FileTypePredictor {
    private static final Logger logger = Logger.getLogger(FileTypePredictor.class.getName());
    private static final String defaultPath = "/magika/model.onnx";

    private final Configuration configuration;
    private final List<String> labels;
    private final byte[] model;

    /**
     * Create a new predictor that uses the default model path and configuration path.
     */
    public FileTypePredictor() {
        configuration = new Configuration();
        labels = configuration.getTargetLabels();
        try (var stream = this.getClass().getResourceAsStream(defaultPath)) {
            Objects.requireNonNull(stream);
            model = stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Predict the file type of the provided file.
     *
     * @param inputFilePath the input file
     * @return the predicted file type
     */
    public Prediction predictFileType(Path inputFilePath) {
        if (!Files.exists(inputFilePath)) {
            logger.warning("Input file path does not exist: " + inputFilePath.toAbsolutePath());
            throw new IllegalArgumentException();
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
        try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {
            try (var session = env.createSession(model, opts)) {
                String inputName = session.getInputNames().iterator().next();
                try (var tensor = OnnxTensor.createTensor(env, inputBuffer)) {
                    try (var result = session.run(Collections.singletonMap(inputName, tensor))) {
                        float[][] outputProbabilities = (float[][]) result.get(0).getValue();
                        int labelIndex = predict(outputProbabilities[0]);
                        return new Prediction(labels.get(labelIndex), outputProbabilities[0][labelIndex]);
                    }
                }
            }
        } catch (OrtException e) {
            throw new IllegalStateException(e);
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

    public Prediction predictBytes(byte[] inputBytes) {
        Path tmpPath;
        try {
            File tmpFile = File.createTempFile("tmp_magika", ".tmp");
            tmpFile.deleteOnExit();
            tmpPath = tmpFile.toPath();
            Files.write(tmpPath, inputBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return predictFileType(tmpPath);
    }

    private static Set<Path> findAllFilesRecursively(Path folder) {
        return findAllFilesRecursively(folder, 999);
    }

    private static Set<Path> findAllFilesRecursively(Path folder, int depth) {
        try (var files = Files.find(folder, depth, (p, bfa) -> bfa.isRegularFile())) {
            return files.collect(Collectors.toSet());
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
        int fileLength = (int) file.length();

        int beginningSize = config.getBeginningSize();
        int midSize = config.getMidSize();
        int endSize = config.getEndSize();
        var bufferSize = beginningSize + midSize + endSize;
        int paddingToken = config.getPaddingToken();

        byte[] beginningBuffer = new byte[Math.min(fileLength, beginningSize)];
        byte[] midBuffer = new byte[Math.min(fileLength, midSize)];
        byte[] endBuffer = new byte[Math.min(fileLength, endSize)];

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

            // write beginning bytes
            raf.read(beginningBuffer);

            // write middle bytes
            int offset;
            if (midSize > 0) {
                // calculate the offset for the middle part that is equally left and right of the middle
                int halfInputSize = Math.round((float) fileLength / 2);
                offset = Math.max(0, halfInputSize - (midSize / 2));
                raf.seek(offset);

                raf.read(midBuffer);
            }

            // write end bytes
            offset = Math.max(0, fileLength - endSize);
            raf.seek(offset);

            raf.read(endBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] inputArray = new int[bufferSize];
        Arrays.fill(inputArray, paddingToken);
        // Beginning chunk
        for (int i = 0; i < beginningBuffer.length; i++) {
            inputArray[i] = Byte.toUnsignedInt(beginningBuffer[i]);
        }

        // Mid chunk
        // note: it is currently not known how/where the padding should be handled
        for (int i = 0; i < midBuffer.length; i++) {
            inputArray[beginningSize + i] = Byte.toUnsignedInt(midBuffer[i]);
        }

        // End chunk. It should end with the file, and padding at the beginning.
        // take care if file is smaller than endSize
        for (int i = 0; i < endBuffer.length; i++) {
            int inputArrayIndex = inputArray.length - 1 - i;
            int endArrayIndex = endBuffer.length - 1 - i;
            inputArray[inputArrayIndex] = Byte.toUnsignedInt(endBuffer[endArrayIndex]);
        }

        int[][] returnArray = new int[1][];
        returnArray[0] = inputArray;
        return returnArray;
    }
}
