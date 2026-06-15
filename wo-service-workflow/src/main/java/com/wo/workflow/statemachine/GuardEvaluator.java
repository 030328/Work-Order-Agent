package com.wo.workflow.statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluates SpEL (Spring Expression Language) guard conditions
 * for workflow transitions.
 */
@Slf4j
@Component
public class GuardEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Evaluate a SpEL guard condition against the given context variables.
     *
     * @param condition the SpEL expression to evaluate
     * @param context   variables available in the expression context
     * @return true if the condition evaluates to true
     */
    public boolean evaluate(String condition, Map<String, Object> context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            if (context != null) {
                context.forEach(evalContext::setVariable);
            }

            Expression expression = parser.parseExpression(condition);
            Boolean result = expression.getValue(evalContext, Boolean.class);
            log.debug("Guard condition '{}' evaluated to: {}", condition, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to evaluate guard condition: {}", condition, e);
            return false;
        }
    }

    /**
     * Evaluate a SpEL expression and return the result as the specified type.
     *
     * @param expression the SpEL expression
     * @param context    variables available in the expression context
     * @param clazz      the expected return type
     * @param <T>        the return type
     * @return the evaluated result
     */
    public <T> T evaluate(String expression, Map<String, Object> context, Class<T> clazz) {
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            if (context != null) {
                context.forEach(evalContext::setVariable);
            }

            Expression spelExpression = parser.parseExpression(expression);
            return spelExpression.getValue(evalContext, clazz);
        } catch (Exception e) {
            log.error("Failed to evaluate expression: {}", expression, e);
            return null;
        }
    }
}
