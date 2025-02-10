package edu.kit.kastel.mcse.ardoco.magika;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Configuration {
    private static final String defaultPath = "src/main/resources/config.json";

    private final String path;
    private JsonNode root = null;

    /**
     * Create a configuration that reads in from the provided path.
     *
     * @param path the path of the configuration file
     */
    public Configuration(Path path) {
        this.path = path.toAbsolutePath().toString();
    }

    /**
     * Create a configuration that reads in from the provided path.
     *
     * @param path the path of the configuration file
     */
    public Configuration(String path) {
        this.path = path;
    }

    /**
     * Create a configuration that reads in from the default configuration file.
     */
    public Configuration() {
        this.path = defaultPath;
    }

    private JsonNode get(String fieldName) {
        if (root == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                this.root = objectMapper.readTree(new File(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return root.get(fieldName);
    }

    private int getIntValue(String fieldName) {
        return get(fieldName).asInt();
    }

    /**
     * Return the list of target labels from the configuration
     *
     * @return the list of target labels from the configuration
     */
    public List<String> getTargetLabels() {
        var field = get("target_labels_space");
        TypeReference<List<String>> typeReferenceList = new TypeReference<List<String>>() {
        };
        return new ObjectMapper().convertValue(field, typeReferenceList);
    }

    /**
     * Get the number of bytes from the beginning of the file.
     *
     * @return the number of bytes from the beginning of the file.
     */
    public int getBeginningSize() {
        return getIntValue("beg_size");
    }

    /**
     * Get the number of bytes from the middle of the file.
     *
     * @return the number of bytes from the middle of the file.
     */
    public int getMidSize() {
        return getIntValue("mid_size");
    }

    /**
     * Get the number of bytes from the end of the file.
     *
     * @return the number of bytes from the end of the file.
     */
    public int getEndSize() {
        return getIntValue("end_size");
    }

    /**
     * Get the padding token.
     *
     * @return Get the padding token.
     */
    public int getPaddingToken() {
        return getIntValue("padding_token");
    }

    /**
     * Get the minimum file size for prediction
     *
     * @return the minimum file size for prediction
     */
    public int getMinSize() {
        return getIntValue("min_file_size_for_dl");
    }
}
