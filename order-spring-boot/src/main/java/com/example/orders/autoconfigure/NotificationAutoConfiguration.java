package com.example.orders.autoconfigure;


import com.example.orders.boot.notification.ConsoleNotificationSender;
import com.example.orders.boot.notification.NotificationSender;
import com.example.orders.boot.notification.SmsNotificationSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class NotificationAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.example.orders.sms.SmsClient")
    static class SmsClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(NotificationSender.class)
        SmsNotificationSender smsNotificationSender() {
            System.out.println("-> [AutoConfig] SmsClient найден: создаём SMS sender");
            return new SmsNotificationSender();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("com.example.orders.sms.SmsClient")
    static class ConsoleConfiguration {

        @Bean
        @ConditionalOnMissingBean(NotificationSender.class)
        ConsoleNotificationSender consoleNotificationSender() {
            System.out.println("-> [AutoConfig] SmsClient отсутствует: создаём Console sender");
            return new ConsoleNotificationSender();
        }
    }
}
