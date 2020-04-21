import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Agency {
    private static int AGENCY_ID;
    private static Channel channel;
    private static String EXCHANGE_NAME = "topic_exchange";

    private static void init() throws IOException, TimeoutException {
        System.out.println("AGENCY");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter Agency ID: ");
        AGENCY_ID = Integer.parseInt(br.readLine());
    }

    private static void initListening() throws IOException {
        String ackQueueName = "agencyQueue" + AGENCY_ID;
        channel.queueDeclare(ackQueueName, true, false, true, null);
        System.out.println("Created queue: " + ackQueueName);

        String key = "space.agency." + AGENCY_ID;
        channel.queueBind(ackQueueName, EXCHANGE_NAME, key);

        // admin broadcast
        channel.queueBind(ackQueueName, EXCHANGE_NAME, "space");

        //admin agency broadcast
        channel.queueBind(ackQueueName, EXCHANGE_NAME, "space.agency");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                String message = new String(body, StandardCharsets.UTF_8);

                if(envelope.getRoutingKey().equals("space")){
                    System.out.println("Received broadcast message from admin: " + message);
                }
                else if(envelope.getRoutingKey().equals("space.agency")){
                    System.out.println("Received agency broadcast message from admin: " + message);
                }
                else{
                    System.out.println("Received ACK for order " + Integer.parseInt(message));
                }
            }
        };

        // start listening
        channel.basicConsume(ackQueueName, true, consumer);
    }

    public static void main(String[] argv) throws Exception {
        init();
        initListening();

        int ORDER_ID = 1;
        while (true) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter service: ");
            String service = br.readLine();
            String key = "";

            if(service.equals("transit")){
                key = "space.carrier.transit";
            }
            else if(service.equals("cargo")){
                key = "space.carrier.cargo";
            }
            else if(service.equals("satellite")){
                key = "space.carrier.satellite";
            }
            else{
                System.out.println("Available services: transit/cargo/satellite");
            }

            if (service.equals("exit")) {
                break;
            }

            if(!key.isEmpty()) {
                channel.basicPublish(EXCHANGE_NAME, key, null, (AGENCY_ID + " " + ORDER_ID).getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent: " + service);
                ORDER_ID++;
            }
        }
    }
}
