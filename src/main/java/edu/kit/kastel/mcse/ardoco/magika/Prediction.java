/* Licensed under Apache 2.0 2025. */
package edu.kit.kastel.mcse.ardoco.magika;

/**
 * A Prediction that contains a label and the corresponding probability.
 *
 * @param label       the label
 * @param probability the probability
 */
public record Prediction(String label, float probability) {
}
