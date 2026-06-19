package com.wo.workflow.statemachine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Executes transition actions by looking up Spring beans by name
 * and invoking their execute method via reflection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionExecutor {

    private final ApplicationContext applicationContext;

    /**
     * Execute a transition action by looking up the bean by name and invoking its execute method.
     *
     * @param actionBeanName the Spring bean name of the action class
     * @param context        the execution context variables
     * @throws Exception if the action execution fails
     */
    public void execute(String actionBeanName, Map<String, Object> context) throws Exception {
        log.info("Executing action bean: {}", actionBeanName);

        Object bean;
        try {
            bean = applicationContext.getBean(actionBeanName);
        } catch (Exception e) {
            log.error("Action bean not found: {}", actionBeanName, e);
            throw new IllegalArgumentException("Action bean not found: " + actionBeanName, e);
        }

        // Try to find an execute method on the bean
        try {
            Method executeMethod = findExecuteMethod(bean.getClass());
            if (executeMethod != null) {
                if (executeMethod.getParameterCount() == 0) {
                    executeMethod.invoke(bean);
                } else {
                    executeMethod.invoke(bean, context);
                }
                log.info("Action '{}' executed successfully", actionBeanName);
            } else {
                log.warn("No execute method found on action bean: {}", actionBeanName);
            }
        } catch (Exception e) {
            log.error("Failed to execute action '{}': {}", actionBeanName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find a suitable execute method on the given class.
     * Looks for: execute(Map), execute(Map<String, Object>), or a no-arg execute().
     *
     * @param clazz the class to inspect
     * @return the execute method, or null if not found
     */
    private Method findExecuteMethod(Class<?> clazz) {
        // Look for execute(Map) method first
        for (Method method : clazz.getMethods()) {
            if ("execute".equals(method.getName())) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Map.class.isAssignableFrom(params[0])) {
                    return method;
                }
            }
        }

        // Fallback to no-arg execute
        try {
            return clazz.getMethod("execute");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
