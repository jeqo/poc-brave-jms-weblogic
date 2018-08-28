package io.github.jeqo.poc;

import javax.jms.*;
import javax.naming.NamingException;

public final class ProducerApp extends BaseApp {

  private ProducerApp() {
    super("jms-producer-1.1");
  }

  private void send(String text) throws NamingException, JMSException {
    final Connection conn = jmsTracing.connectionFactory(connectionFactory()).createConnection();
    final Connection connection = jmsTracing.connection(conn);
    try (Session session = connection.createSession()) {
      Destination queue = session.createQueue(destinationName);
      MessageProducer messageProducer = session.createProducer(queue);

      Message message = session.createTextMessage(text);

      messageProducer.send(message);

      System.out.println("Message sent");
    }
  }
  public static void main(String[] args) throws JMSException, InterruptedException, NamingException {
    ProducerApp app = new ProducerApp();

    app.send("Hola mundo");

    Thread.sleep(10_000L);
  }
}