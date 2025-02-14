/* Licensed under Apache 2.0 2025. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Configuration {
    private static final String MAGIKA_CONFIG_JSON = "/magika/config.json";
    private JsonNode root = null;

    private JsonNode get(String fieldName) {
        if (root == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                this.root = objectMapper.readTree(this.getClass().getResourceAsStream(MAGIKA_CONFIG_JSON));
            } catch (IOException e) {
                throw new IllegalStateException(e);
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
        TypeReference<List<String>> typeReferenceList = new TypeReference<>() {
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
