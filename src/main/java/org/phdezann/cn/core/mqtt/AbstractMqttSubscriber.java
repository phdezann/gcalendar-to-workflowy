package org.phdezann.cn.core.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMqttSubscriber {

    private final List<String> topics;
    private final MqttClient client;
    private final String username;
    private final String password;

    protected AbstractMqttSubscriber(String serverUri, String username, String password, List<String> topics) {
        this.topics = topics;
        this.username = username;
        this.password = password;
        var randomId = UUID.randomUUID().toString();
        this.client = newClient(serverUri, randomId);
    }

    private MqttClient newClient(String serverUri, String randomId) {
        try {
            return new MqttClient(serverUri, randomId, null);
        } catch (MqttException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void startReadingMessagesAsync() {
        try {
            this.client.setCallback(buildCallback());
            var options = new MqttConnectionOptionsBuilder().automaticReconnect(true);
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                options = options //
                        .username(username) //
                        .password(password.getBytes(StandardCharsets.UTF_8));
            }
            this.client.connect(options.build());
            subscribe();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void subscribe() {
        try {
            var topicArray = this.topics.toArray(new String[0]);
            var array = IntStream.range(0, this.topics.size()).map(i -> 0).toArray();
            this.client.subscribe(topicArray, array);
            log.debug("Subscribe has been called");
        } catch (MqttException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MqttClientCallback buildCallback() {
        return new MqttClientCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                AbstractMqttSubscriber.this.messageArrived(topic, message);
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                log.debug("Callback connectComplete called with reconnect:{} and serverURI:{}", reconnect, serverURI);
                // see https://github.com/eclipse/paho.mqtt.java/issues/576#issuecomment-866892911
                AbstractMqttSubscriber.this.subscribe();
            }
        };
    }

    protected abstract void messageArrived(String topic, MqttMessage message);

    public void shutdown() {
        try {
            this.client.disconnectForcibly(1000, 1000, false);
            this.client.close();
        } catch (MqttException ex) {
            log.error("Error while closing client", ex);
        }
    }

}
