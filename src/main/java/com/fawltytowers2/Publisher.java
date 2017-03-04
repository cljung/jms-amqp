/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package example;

import org.apache.qpid.jms.*;
import javax.jms.*;
import java.util.Random;

class Publisher {

    public static void main(String[] args) throws JMSException {

        // get connection string from environment variables
        String ConnectionURI = GetConnectionURI( args );
        System.out.println("ConnectionURI: "+ConnectionURI);

        // get queue name and message details from command line
        String destinationName = arg(args, 0, "topic://event");
        String msg = arg( args, 1, "Hello from Java App sending a test AMQP message via JMS to Azure ServiceBus Queue");
        int loopCount = Integer.valueOf( arg( args, 2, "1") );

        // connect to AMQP host
        JmsConnectionFactory factory = new JmsConnectionFactory( ConnectionURI );
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // create a session to the queue/topic
        String TOPIC_PREFIX = "topic://";
        Destination destination = null;
        if (destinationName.startsWith(TOPIC_PREFIX)) {
            destination = session.createTopic(destinationName.substring(TOPIC_PREFIX.length()));
        } else {
            destination = session.createQueue(destinationName);
        }

        // create the sender
        MessageProducer sender = session.createProducer(destination);
        System.out.println(sender.getDestination());
        System.out.println("sender: "+sender);

        // send N number of messages
        Random randomGenerator = new Random();
        long start = System.currentTimeMillis();
        System.out.println("Sending messages...");

        for( int n = 1; n <= loopCount; n++ ) {
            TextMessage message = session.createTextMessage();
            message.setText("[" + n + "] " + msg);
            long randomMessageID = randomGenerator.nextLong() >>> 1;
            message.setJMSMessageID("ID:" + randomMessageID);
            sender.send(message);
            System.out.println("JMSMessageID = " + message.getJMSMessageID());
            System.out.println("Text = " + message.getText());
        }

        long diff = System.currentTimeMillis() - start;
        System.out.println(String.format("%.2f seconds", (1.0 * diff / 1000.0)));

        connection.close();
    }

    private static String GetConnectionURI( String[] args ) {
        String user = env("JMS_USER", "admin");
        String password = env("JMS_PASSWORD", "password");
        String host = env("JMS_HOST", "localhost");
        String protocol = "amqp";
        int port = Integer.parseInt(env("JMS_PORT", "5672"));
        if ( port == 5671 ) {
            protocol = "amqps";
        }
        String connectTimeout = env("JMS_CONNECTTIMEOUT", "");
        String idleTimeout = env("JMS_IDLETIMEOUT", "");
        String timeouts = "";
        if ( connectTimeout.length() > 0 ) {
            timeouts += "&transport.connectTimeout=" + connectTimeout;
        }
        if ( idleTimeout.length() > 0 ) {
            timeouts += "&amqp.idleTimeout=" + idleTimeout;
        }
        String ConnectionURI = protocol + "://" + host + ":" + port + "/"
                + "?jms.username=" + user
                + "&jms.password=" + password
                + timeouts;
        return ConnectionURI;
    }

    private static String env(String key, String defaultValue) {
        String rc = System.getenv(key);
        if (rc == null)
            return defaultValue;
        return rc;
    }

    private static String arg(String[] args, int index, String defaultValue) {
        if (index < args.length)
            return args[index];
        else
            return defaultValue;
    }
}