package edu.kit.kastel.mcse.ardoco.magika;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileTypePredictorTest {
    private static final Logger logger = Logger.getLogger(FileTypePredictorTest.class.getName());

    private final FileTypePredictor predictor = new FileTypePredictor();

    @Test
    void predictFileTypeTest() {
        var input = Paths.get("src/main/resources/config.json");
        var expectedType = "json";
        validatePrediction(input, expectedType);

        input = Paths.get(".gitignore");
        expectedType = "ignorefile";
        validatePrediction(input, expectedType);

        input = Paths.get("LICENSE");
        expectedType = "txt";
        validatePrediction(input, expectedType);

        input = Paths.get("pom.xml");
        expectedType = "xml";
        validatePrediction(input, expectedType);

        input = Paths.get("src/main/java/edu/kit/kastel/mcse/ardoco/magika/FileTypePredictor.java");
        expectedType = "java";
        validatePrediction(input, expectedType);
    }

    private void validatePrediction(Path input, String expectedType) {
        var prediction = predictor.predictFileType(input);
        var predictionString = prediction.label();
        Assertions.assertEquals(expectedType, predictionString);
    }

    @Test
    void predictFileTypesFromFolderRecursivelyTest() {
        var input = Paths.get("src/main/resources");

        var resultMap = predictor.predictFileTypesFromFolderRecursively(input);
        Assertions.assertEquals(resultMap.size(), 3);

        input = Paths.get("src/main/java");

        resultMap = predictor.predictFileTypesFromFolderRecursively(input);
        String java = "java";
        for (var prediction : resultMap.entrySet()) {
            Assertions.assertEquals(prediction.getValue().label(), java);
        }
    }
}
