package com.octopuz.selectionservice.aspect;

import com.octopuz.selectionservice.annotation.LogSelection;
import com.octopuz.selectionservice.context.UserContext;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;
import com.octopuz.selectionservice.producer.LogMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SelectionLogAspect {

    @Autowired
    private LogMessageProducer logMessageProducer;

    @Around("@annotation(logSelection)")
    public Object around(ProceedingJoinPoint joinPoint, LogSelection logSelection) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof SelectionResponse) {
            SelectionResponse response = (SelectionResponse) result;
            if (response.getSuccess()) {
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    if (arg instanceof SelectionRequest) {
                        SelectionRequest request = (SelectionRequest) arg;
                        String operator = UserContext.getOperator();
                        logMessageProducer.sendLogMessage(
                                request.getStudentNo(),
                                request.getCourseNo(),
                                operator,
                                logSelection.action()
                        );
                        break;
                    }
                }
            }
        }
        return result;
    }
}