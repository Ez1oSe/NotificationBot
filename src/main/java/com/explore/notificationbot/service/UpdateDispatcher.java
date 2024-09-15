package com.explore.notificationbot.service;

import com.explore.notificationbot.repository.UserRepo;
import com.explore.notificationbot.service.handler.CallbackQueryHandler;
import com.explore.notificationbot.service.handler.CommandHandler;
import com.explore.notificationbot.service.handler.MessageHandler;
import com.explore.notificationbot.bot.Bot;
import com.explore.notificationbot.entity.Action;
import com.explore.notificationbot.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateDispatcher {
    MessageHandler messageHandler;
    CommandHandler commandHandler;
    CallbackQueryHandler queryHandler;
    UserRepo userRepo;
    public BotApiMethod<?> distribute(Update update, Bot bot) {
        try {
            if (update.hasCallbackQuery()) {
                checkUser(update.getCallbackQuery().getMessage().getChatId());
                return queryHandler.answer(update.getCallbackQuery(), bot);
            }
            if (update.hasMessage()) {
                var message = update.getMessage();
                checkUser(message.getChatId());
                if (message.hasText()) {
                    if (message.getText().charAt(0) == '/') {
                        return commandHandler.answer(message, bot);
                    }
                    return messageHandler.answer(message, bot);
                }
            }
            log.warn("Unsupported update type: " + update);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private void checkUser(Long chatId) {
        if (userRepo.existsByChatId(chatId)) {
            return;
        }
        userRepo.save(
                User.builder()
                        .action(Action.FREE)
                        .registeredAt(LocalDateTime.now())
                        .chatId(chatId)
                        .firstName("Yo")
                        .build()
        );
    }

}
