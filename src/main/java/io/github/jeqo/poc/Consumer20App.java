package io.github.jeqo.poc;

import javax.jms.*;
import javax.naming.NamingException;

import static java.lang.System.out;

public final class Consumer20App extends BaseApp {

  private Consumer20App() {
    super("jms-consumer-2.0");
  }

  private void consume() throws NamingException, JMSException {
    try (JMSContext context = jmsTracing.connectionFactory(connectionFactory()).createContext()) {
      Destination queue = context.createQueue(destinationName);
      JMSConsumer consumer = context.createConsumer(queue);
      // consumer.receiveBody does not support propagation of tracing context.
      TextMessage body = (TextMessage) consumer.receive();
      out.println("Message received: " + body.getText());
    }
  }

  public static void main(String[] args) throws NamingException, InterruptedException, JMSException {
    Consumer20App app = new Consumer20App();

    app.consume();

    Thread.sleep(10_000L);
  }
}
