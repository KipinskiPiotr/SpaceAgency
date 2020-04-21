import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Admin {
    public static void main(String[] argv) throws Exception {
        // info
        System.out.println("ADMIN");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String EXCHANGE_NAME = "topic_exchange";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String key = "space.#";
        String queueName = "adminQueue";

        channel.queueDeclare(queueName, true, false, true, null);
        channel.queueBind(queueName, EXCHANGE_NAME, key);
        System.out.println("Created queue: " + queueName);

        // consumer (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Received msg: " + message + " with routing key: " + envelope.getRoutingKey());
            }
        };

        // start listening
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter message: ");
            String message = br.readLine();

            System.out.println("Enter key for the message: ");
            key = br.readLine();

            // publish
            channel.basicPublish(EXCHANGE_NAME, key, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Sent: " + message);
        }
    }
}
