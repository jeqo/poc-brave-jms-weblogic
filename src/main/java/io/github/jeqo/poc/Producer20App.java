package io.github.jeqo.poc;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.naming.NamingException;

public final class Producer20App extends BaseApp {
  private Producer20App() {
    super("jms-producer-2.0");
  }

  private void send(String text) throws NamingException {
    try (JMSContext context = jmsTracing.connectionFactory(connectionFactory()).createContext()) {
      Destination queue = context.createQueue(destinationName);
      context.createProducer().send(queue, text);
    }
  }

  public static void main(String[] args) throws InterruptedException, NamingException {
    Producer20App app = new Producer20App();

    app.send("Hola mundo");

    Thread.sleep(10_000L);
  }
}