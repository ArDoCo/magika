/* Licensed under Apache 2.0 2025. */
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

    @Test
    void predictBytes() {
        String content = """
                
                .idea
                
                # User-specific stuff
                .idea/**/workspace.xml
                .idea/**/tasks.xml
                .idea/**/usage.statistics.xml
                .idea/**/dictionaries
                .idea/**/shelf
                
                # AWS User-specific
                .idea/**/aws.xml
                
                # Generated files
                .idea/**/contentModel.xml
                
                # Sensitive or high-churn files
                .idea/**/dataSources/
                .idea/**/dataSources.ids
                .idea/**/dataSources.local.xml
                .idea/**/sqlDataSources.xml
                .idea/**/dynamic.xml
                .idea/**/uiDesigner.xml
                .idea/**/dbnavigator.xml
                
                # Gradle
                .idea/**/gradle.xml
                .idea/**/libraries
                
                # Gradle and Maven with auto-import
                # When using Gradle or Maven with auto-import, you should exclude module files,
                # since they will be recreated, and may cause churn.  Uncomment if using
                # auto-import.
                # .idea/artifacts
                # .idea/compiler.xml
                # .idea/jarRepositories.xml
                # .idea/modules.xml
                # .idea/*.iml
                # .idea/modules
                # *.iml
                # *.ipr
                
                # CMake
                cmake-build-*/
                
                """;
        byte[] input = content.getBytes();
        var result = predictor.predictBytes(input);
        Assertions.assertEquals("ignorefile", result.label());
    }
}
