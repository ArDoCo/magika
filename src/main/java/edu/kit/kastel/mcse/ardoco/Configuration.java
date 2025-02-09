package edu.kit.kastel.mcse.ardoco;

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

    public Configuration(Path path) {
        this.path = path.toAbsolutePath().toString();
    }

    public Configuration(String path) {
        this.path = path;
    }

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

    public List<String> getTargetLabels() {
        var field = get("target_labels_space");
        TypeReference<List<String>> typeReferenceList = new TypeReference<List<String>>() {};
        return new ObjectMapper().convertValue(field, typeReferenceList);
    }

    public int getBeginningSize() {
        return getIntValue("beg_size");
    }

    public int getMidSize() {
        return getIntValue("mid_size");
    }

    public int getEndSize() {
        return getIntValue("end_size");
    }

    public int getPaddingToken() {
        return getIntValue("padding_token");
    }
}
