import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Carrier {
    private static String EXCHANGE_NAME = "topic_exchange";
    private static Channel channel;
    private static int CARRIER_ID;

    private static void init() throws IOException, TimeoutException {
        System.out.println("CARRIER");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter Carrier ID: ");
        CARRIER_ID = Integer.parseInt(br.readLine());
    }

    private static Consumer getConsumer(String service){
        switch (service){
            case "transit":
                return new TransitConsumer(channel, EXCHANGE_NAME);
            case "cargo":
                return new CargoConsumer(channel, EXCHANGE_NAME);
            case "satellite":
                return new SatelliteConsumer(channel, EXCHANGE_NAME);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void initQueues() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String service1 = null;

        do{
            if(service1 != null){
                System.out.println("Available services: transit/cargo/satellite");
            }
            System.out.println("Enter 1st service: ");
            service1 = br.readLine();
        }while(!service1.equals("transit") && !service1.equals("cargo") && !service1.equals("satellite"));

        String service2 = null;

        do{
            if(service2 != null){
                System.out.println("Available services: transit/cargo/satellite");
            }
            System.out.println("Enter 2nd service: ");
            service2 = br.readLine();
        }while(!service2.equals("transit") && !service2.equals("cargo") && !service2.equals("satellite") || service2.contains(service1));

        String key1 = "space.carrier." + service1;
        String key2 = "space.carrier." + service2;

        String queueName1 = service1 + "Queue";
        String queueName2 = service2 + "Queue";

        channel.queueDeclare(queueName1, true, false, true, null);
        channel.queueBind(queueName1, EXCHANGE_NAME, key1);
        System.out.println("Created queue: " + queueName1);

        channel.queueDeclare(queueName2, true, false, true, null);
        channel.queueBind(queueName2, EXCHANGE_NAME, key2);
        System.out.println("Created queue: " + queueName2);

        // admin broadcast
        String queueName3 = "carrierQueue" + CARRIER_ID;
        channel.queueDeclare(queueName3, true, false, true, null);
        channel.queueBind(queueName3, EXCHANGE_NAME, "space");
        channel.queueBind(queueName3, EXCHANGE_NAME, "space.carrier");
        System.out.println("Created queue: " + queueName3);

        Consumer consumer3 = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                String message = new String(body, StandardCharsets.UTF_8);
                if(envelope.getRoutingKey().equals("space")) {
                    System.out.println("Received broadcast message from admin: " + message);
                }
                else{
                    System.out.println("Received carrier broadcast message from admin: " + message);
                }
            }
        };

        Consumer consumer1 = getConsumer(service1);
        Consumer consumer2 = getConsumer(service2);

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName1, true, consumer1);
        channel.basicConsume(queueName2, true, consumer2);
        channel.basicConsume(queueName3, true, consumer3);
    }

    public static void main(String[] argv) throws Exception {
        init();
        initQueues();
    }
}
