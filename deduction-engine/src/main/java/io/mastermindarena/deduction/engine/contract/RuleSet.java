package io.mastermindarena.deduction.engine.contract;

public interface RuleSet {
    ActionResolution resolve(Object actionInput);
}
