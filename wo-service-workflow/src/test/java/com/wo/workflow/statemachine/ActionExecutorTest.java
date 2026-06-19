package com.wo.workflow.statemachine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionExecutorTest {

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final ActionExecutor actionExecutor = new ActionExecutor(applicationContext);

    @Test
    void executeShouldInvokeMapBasedActionBean() throws Exception {
        RecordingAction action = new RecordingAction();
        Map<String, Object> context = Map.of("workOrderId", 1001L);
        when(applicationContext.getBean("recordingAction")).thenReturn(action);

        actionExecutor.execute("recordingAction", context);

        assertThat(action.executed.get()).isTrue();
        assertThat(action.receivedContext.get()).isEqualTo(context);
    }

    @Test
    void executeShouldInvokeNoArgActionBeanWhenMapMethodIsAbsent() throws Exception {
        NoArgAction action = new NoArgAction();
        when(applicationContext.getBean("noArgAction")).thenReturn(action);

        actionExecutor.execute("noArgAction", Map.of());

        assertThat(action.executed.get()).isTrue();
    }

    @Test
    void missingActionBeanShouldThrowHelpfulException() {
        when(applicationContext.getBean("missingAction"))
                .thenThrow(new NoSuchBeanDefinitionException("missingAction"));

        assertThatThrownBy(() -> actionExecutor.execute("missingAction", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Action bean not found");
    }

    static class RecordingAction {
        private final AtomicBoolean executed = new AtomicBoolean(false);
        private final AtomicReference<Map<String, Object>> receivedContext = new AtomicReference<>();

        public void execute(Map<String, Object> context) {
            executed.set(true);
            receivedContext.set(context);
        }
    }

    static class NoArgAction {
        private final AtomicBoolean executed = new AtomicBoolean(false);

        public void execute() {
            executed.set(true);
        }
    }
}
