package com.devbridge.logicore.message;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageService {

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, Object... args) {
        Object[] text = new Object[args == null ? 0 : args.length];
        for (int i = 0; i < text.length; i++) {
            text[i] = String.valueOf(args[i]);   // so id 1024 is not printed as "1,024"
        }
        return messageSource.getMessage(key, text, key, LocaleContextHolder.getLocale());
    }
}