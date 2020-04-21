import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TransitConsumer extends DefaultConsumer {
    private Channel channel;
    private final String EXCHANGE_NAME;

    public TransitConsumer(Channel channel, String exchangeName) {
        super(channel);
        this.channel = channel;
        this.EXCHANGE_NAME = exchangeName;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        String message = new String(body, StandardCharsets.UTF_8);

        String[] data = message.split(" ");
        String AGENCY_ID = data[0];
        String ORDER_ID = data[1];

        System.out.println("Received transit order " + ORDER_ID + " from Agency " + AGENCY_ID);

        String key = "space.agency." + Integer.parseInt(AGENCY_ID);
        channel.basicPublish(EXCHANGE_NAME, key, null, ORDER_ID.getBytes(StandardCharsets.UTF_8));
        System.out.println("Sent ACK");
    }
}
