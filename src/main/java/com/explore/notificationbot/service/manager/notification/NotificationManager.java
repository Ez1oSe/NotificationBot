package com.explore.notificationbot.service.manager.notification;

import com.explore.notificationbot.repository.NotificationRepo;
import com.explore.notificationbot.repository.UserRepo;
import com.explore.notificationbot.service.contract.AbstractManager;
import com.explore.notificationbot.service.contract.CommandListener;
import com.explore.notificationbot.service.contract.MessageListener;
import com.explore.notificationbot.service.contract.QueryListener;
import com.explore.notificationbot.service.factory.KeyboardFactory;
import com.explore.notificationbot.bot.Bot;
import com.explore.notificationbot.entity.Action;
import com.explore.notificationbot.entity.Notification;
import com.explore.notificationbot.entity.Status;
import com.explore.notificationbot.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.explore.notificationbot.data.CallbackData.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationManager extends AbstractManager
        implements QueryListener, CommandListener, MessageListener {
    KeyboardFactory keyboardFactory;
    NotificationRepo notificationRepo;
    UserRepo userRepo;

    @Override
    public BotApiMethod<?> mainMenu(Message message, Bot bot) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text("Настройте уведомление")
                .replyMarkup(
                        editNotificationReplyMarkup(
                                String.valueOf(
                                        userRepo.findByChatId(message.getChatId())
                                                .getCurrentNotification()
                                )
                        )
                )
                .build();
    }

    @Override
    public BotApiMethod<?> mainMenu(CallbackQuery query, Bot bot) {
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("###")
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("Добавить уведомление"),
                                List.of(1),
                                List.of(notification_new.name())
                        )
                )
                .build();
    }

    @Override
    public BotApiMethod<?> answerCommand(Message message, Bot bot) {
        return null;
    }

    @Override
    public BotApiMethod<?> answerMessage(Message message, Bot bot) throws TelegramApiException {
        var user = userRepo.findByChatId(message.getChatId());
        bot.execute(
                DeleteMessage.builder()
                        .chatId(message.getChatId())
                        .messageId(message.getMessageId() - 1)
                        .build()
        );
        switch (user.getAction()) {
            case SENDING_TIME -> {
                return editTime(message, user, bot);
            }
            case SENDING_DESCRIPTION -> {
                return editDescription(message, user, bot);
            }
            case SENDING_TITLE -> {
                return editTitle(message, user, bot);
            }
        }
        return null;
    }

    private BotApiMethod<?> editTitle(Message message, User user, Bot bot) {
        var notification = notificationRepo.findById(user.getCurrentNotification()).orElseThrow();
        notification.setTitle(message.getText());
        notificationRepo.save(notification);

        user.setAction(Action.FREE);
        userRepo.save(user);
        return mainMenu(message, bot);
    }

    private BotApiMethod<?> editDescription(Message message, User user, Bot bot) {
        var notification = notificationRepo.findById(user.getCurrentNotification()).orElseThrow();
        notification.setDescription(message.getText());
        notificationRepo.save(notification);

        user.setAction(Action.FREE);
        userRepo.save(user);
        return mainMenu(message, bot);
    }

    private BotApiMethod<?> editTime(Message message, User user, Bot bot) {
        var notification = notificationRepo.findById(user.getCurrentNotification()).orElseThrow();

        var messageText = message.getText().strip();
        var pattern = Pattern.compile("^[0-9]{2}:[0-9]{2}:[0-9]{2}$").matcher(messageText);
        if (pattern.matches()) {
            var nums = messageText.split(":");
            int seconds = Integer.parseInt(nums[0]) * 3600 + Integer.parseInt(nums[1]) * 60 + Integer.parseInt(nums[2]);
            notification.setSeconds(seconds);
        } else {
            return SendMessage.builder()
                    .text("Некорректный формат ввода\nЧЧ:ММ:СС (01:00:30 - один час, ноль минут, тридцать секунд)")
                    .chatId(message.getChatId())
                    .replyMarkup(
                            keyboardFactory.createInlineKeyboard(
                                    List.of("\uD83D\uDD19 Назад"),
                                    List.of(1),
                                    List.of(notification_back_ + String.valueOf(user.getCurrentNotification()))
                            )
                    )
                    .build();
        }
        notificationRepo.save(notification);
        user.setAction(Action.FREE);
        userRepo.save(user);
        return mainMenu(message, bot);
    }

    @Override
    public BotApiMethod<?> answerQuery(CallbackQuery query, String[] words, Bot bot) throws TelegramApiException {
        switch (words.length) {
            case 2 -> {
                switch (words[1]) {
                    case "main" -> {
                        return mainMenu(query, bot);
                    }
                    case "new" -> {
                        return newNotification(query, bot);
                    }
                }
            }
            case 3 -> {
                switch (words[1]) {
                    case "back" -> {
                        return editPage(query, words[2]);
                    }
                    case "done" -> {
                        return sendNotification(query, words[2], bot);
                    }
                }
            }
            case 4 -> {
                switch (words[1]) {
                    case "edit" -> {
                        switch (words[2]) {
                            case "title" -> {
                                return askTitle(query, words[3]);
                            }
                            case "d" -> {
                                return askDescription(query, words[3]);
                            }
                            case "time" -> {
                                return askSeconds(query, words[3]);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BotApiMethod<?> sendNotification(CallbackQuery query, String id, Bot bot) throws TelegramApiException {
        var notification = notificationRepo.findById(UUID.fromString(id)).orElseThrow();
        if (notification.getTitle() == null  || notification.getTitle().isBlank() || notification.getSeconds() == null) {
            return AnswerCallbackQuery.builder()
                            .callbackQueryId(query.getId())
                            .text("Заполните обязательные значения: Заголовок и Время")
                            .build();
        }
        bot.execute(
                AnswerCallbackQuery.builder()
                        .text("Уведомление придет к вам через " + notification.getSeconds() + " секунд \uD83D\uDC40")
                        .callbackQueryId(query.getId())
                        .build()
        );
        notification.setStatus(Status.WAITING);
        notificationRepo.save(notification);
        Thread.startVirtualThread(
                new NotificationContainer(
                        bot,
                        query.getMessage().getChatId(),
                        notification,
                        notificationRepo
                )
        );
        return EditMessageText.builder()
                .text("✅ Успешно")
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("На главную"),
                                List.of(1),
                                List.of(main.name())

                        )
                )
                .build();
    }

    private BotApiMethod<?> editPage(CallbackQuery query, String id) {
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("Настройте уведомление")
                .replyMarkup(editNotificationReplyMarkup(id))
                .build();
    }

    private BotApiMethod<?> askSeconds(CallbackQuery query, String id) {
        var user = userRepo.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_TIME);
        user.setCurrentNotification(UUID.fromString(id));
        userRepo.save(user);
        return EditMessageText.builder()
                .text("⚡\uFE0F Введите время, по прошествии которого хотите получить напоминание\nФормат - ЧЧ:ММ:СС\nНапример - (01:30:00) - полтора часа")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("\uD83D\uDD19 Назад"),
                                List.of(1),
                                List.of(notification_back_ + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> askDescription(CallbackQuery query, String id) {
        var user = userRepo.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_DESCRIPTION);
        user.setCurrentNotification(UUID.fromString(id));
        userRepo.save(user);
        return EditMessageText.builder()
                .text("⚡\uFE0F Добавьте или измените описание, просто напишите в чат тест, который бы хотели получить")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("\uD83D\uDD19 Назад"),
                                List.of(1),
                                List.of(notification_back_ + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> askTitle(CallbackQuery query, String id) {
        var user = userRepo.findByChatId(query.getMessage().getChatId());
        user.setAction(Action.SENDING_TITLE);
        user.setCurrentNotification(UUID.fromString(id));
        userRepo.save(user);
        return EditMessageText.builder()
                .text("⚡\uFE0F Опишите краткий заголовок в следующем сообщение, чтобы вам было сразу понятно, что я вам напоминаю")
                .messageId(query.getMessage().getMessageId())
                .chatId(query.getMessage().getChatId())
                .replyMarkup(
                        keyboardFactory.createInlineKeyboard(
                                List.of("\uD83D\uDD19 Назад"),
                                List.of(1),
                                List.of(notification_back_ + id)
                        )
                )
                .build();
    }

    private BotApiMethod<?> newNotification(CallbackQuery query, Bot bot) {
        var user = userRepo.findByChatId(query.getMessage().getChatId());
        String id = String.valueOf(notificationRepo.save(
                Notification.builder()
                        .user(user)
                        .status(Status.BUILDING)
                        .build()
        ).getId());
        return EditMessageText.builder()
                .chatId(query.getMessage().getChatId())
                .messageId(query.getMessage().getMessageId())
                .text("Настройте уведомление")
                .replyMarkup(editNotificationReplyMarkup(id))
                .build();

    }

    private InlineKeyboardMarkup editNotificationReplyMarkup(String id) {
        List<String> text = new ArrayList<>();
        var notification = notificationRepo.findById(UUID.fromString(id)).orElseThrow();
        if (notification.getTitle() != null && !notification.getTitle().isBlank()) {
            text.add("✅ Заголовок");
        } else {
            text.add("❌ Заголовок");
        }
        if (notification.getSeconds() != null && notification.getSeconds() != 0) {
            text.add("✅ Время");
        } else {
            text.add("❌ Время");
        }
        if (notification.getDescription() != null && !notification.getDescription().isBlank()) {
            text.add("✅ Описание");
        } else {
            text.add("❌ Описание");
        }
        text.add("\uD83D\uDD19 Главная");
        text.add("\uD83D\uDD50 Готово");
        return keyboardFactory.createInlineKeyboard(
                text,
                List.of(2, 1, 2),
                List.of(
                        notification_edit_title_.name() + id, notification_edit_time_.name() + id,
                        notification_edit_d_.name() + id,
                        main.name(), notification_done_.name() + id
                )
        );
    }
}
