package com.github.tonydeng.tcp.client;

import com.github.tonydeng.tcp.ThriftClient;
import com.github.tonydeng.tcp.impl.ThriftClientImpl;
import com.github.tonydeng.tcp.pool.ThriftServerInfo;
import com.github.tonydeng.tcp.service.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by tonydeng on 15/9/28.
 */
public class ThriftClientTest {
    private static final Logger log = LoggerFactory.getLogger(ThriftClientTest.class);

    private static final Supplier<List<ThriftServerInfo>> serverList = () -> Arrays.asList(
            ThriftServerInfo.of("localhost", 9001)
    );

    @Test
    public void testTransport() throws TException {
        Function<TTransport, TProtocol> protocolProvider = transport -> {
            TCompactProtocol  compactProtocol = new TCompactProtocol(transport);
            TMultiplexedProtocol protocol = new TMultiplexedProtocol(compactProtocol, "pingPongService");
            return protocol;
        };

        ThriftClient client = new ThriftClientImpl(serverList);
        Ping ping = new Ping("hi!");
        Pong pong =  client.iface(PingPongService.Client.class,protocolProvider,0).knock(ping);
        log.info("ping message:'{}'  pong answer:'{}'", ping.getMessage(), pong.getAnswer());
    }

    //    @Test
    public void testClientPoolPing() throws InterruptedException {

        ThriftClient client = new ThriftClientImpl(serverList);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 100; i++) {
            int counter = i;

            executorService.submit(() -> {
                try {
                    Ping ping = new Ping("hi " + counter + "!");
//                    Pong pong = client.iface(PingPongService.Client.class).knock(ping);
                    Pong pong = client.iface(PingPongService.Client.class).knock(ping);
                    log.info("ping message:'{}'  pong answer:'{}'", ping.getMessage(), pong.getAnswer());
                } catch (TException e) {
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();

        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    //    @Test
    public void testPing() throws TException {
        log.info("test ping start....");

        TTransport transport = new TSocket("localhost", 9001);

        TProtocol protocol = new TCompactProtocol(transport);

        PingPongService.Client client = new PingPongService.Client(new TMultiplexedProtocol(protocol, "pingPongService"));

        transport.open();
        Ping ping = new Ping("hello world!");
        Pong pong = client.knock(ping);

        log.info("ping:'{}'  pong:'{}'", ping, pong);
        transport.close();
    }

    //    @Test
    public void testMail() throws TException {
        log.info("test mail start....");
        TTransport transport = new TSocket("localhost", 9001);

        TProtocol protocol = new TCompactProtocol(transport);
//        TProtocol protocol = new TBinaryProtocol(transport);

        MailService.Client client = new MailService.Client(new TMultiplexedProtocol(protocol, "mailService"));

        List<Recipient> recipients = new ArrayList<>();
        recipients.add(new Recipient("tonydeng", "dengtao@cim120.com.cn", "15201669909"));

        transport.open();

        client.sendMails(recipients, "subject", "content");

        transport.close();
    }
}
