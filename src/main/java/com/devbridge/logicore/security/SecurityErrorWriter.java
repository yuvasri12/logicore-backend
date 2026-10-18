package com.devbridge.logicore.security;

import com.devbridge.logicore.config.RequestIdFilter;
import com.devbridge.logicore.dto.ApiErrorResponse;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.message.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final MessageService messages;

    public SecurityErrorWriter(ObjectMapper objectMapper, MessageService messages) {
        this.objectMapper = objectMapper;
        this.messages = messages;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                code.getStatus().value(),
                code.getStatus().getReasonPhrase(),
                code.name(),
                messages.get(code.getMessageKey()),
                Collections.<String>emptyList(),
                request.getRequestURI(),
                MDC.get(RequestIdFilter.MDC_KEY));
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}