package com.mailreader;

import com.mailreader.mail.MailMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MailReaderMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailReaderMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider mailTools(MailMcpTools mailMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(mailMcpTools).build();
    }
}
