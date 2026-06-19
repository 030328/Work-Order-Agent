package com.wo.workflow.statemachine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GuardEvaluatorTest {

    private final GuardEvaluator guardEvaluator = new GuardEvaluator();

    @Test
    void emptyConditionShouldPass() {
        assertThat(guardEvaluator.evaluate("", Map.of())).isTrue();
        assertThat(guardEvaluator.evaluate(null, Map.of())).isTrue();
    }

    @Test
    void conditionShouldUseContextVariables() {
        Map<String, Object> context = Map.of(
                "operatorRole", "MANAGER",
                "priority", "HIGH"
        );

        boolean result = guardEvaluator.evaluate(
                "#operatorRole == 'MANAGER' and #priority == 'HIGH'",
                context
        );

        assertThat(result).isTrue();
    }

    @Test
    void conditionShouldFailWhenContextDoesNotMatch() {
        Map<String, Object> context = Map.of(
                "operatorRole", "USER",
                "priority", "LOW"
        );

        boolean result = guardEvaluator.evaluate(
                "#operatorRole == 'MANAGER' and #priority == 'HIGH'",
                context
        );

        assertThat(result).isFalse();
    }

    @Test
    void invalidExpressionShouldFailClosed() {
        assertThat(guardEvaluator.evaluate("#missing +", Map.of())).isFalse();
    }
}
