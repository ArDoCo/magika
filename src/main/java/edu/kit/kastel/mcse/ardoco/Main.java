package edu.kit.kastel.mcse.ardoco;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        FileTypePredictor predictor = new FileTypePredictor();

//        var inputFiles = List.of("src/main/resources/config.json", "src/main/java/edu/kit/kastel/mcse/ardoco/FileTypePredictor.java", "pom.xml", ".gitignore");
//
//        for (var inputFileString : inputFiles) {
//            var input = Paths.get(inputFileString);
//            var predictionString = predictor.predictFileType(input).toString();
//            logger.info(input + ": " + predictionString);
//        }

        var inputFolder = Path.of(".");
        var predictions = predictor.predictFileTypesFromFolderRecursively(inputFolder);
        for (var entry : predictions.entrySet()) {
            var pathString = entry.getKey().toString();
            var predicitonString = entry.getValue().toString();
            logger.info(pathString + ": " + predicitonString);
        }
    }
}
