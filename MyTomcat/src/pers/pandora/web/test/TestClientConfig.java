package pers.pandora.web.test;

import pers.pandora.om.annotation.Bean;
import pers.pandora.om.annotation.Configuration;
import pers.pandora.web.servlet.TestServlet;

@Deprecated
@Configuration
public class TestClientConfig {

    @Bean
    public TestServlet testServlet() {
        TestServlet testServlet = new TestServlet();
        return testServlet;
    }

    @Bean
    public Client client() {
        Client client = new Client();
        client.setTestOM("test @Bean Client autowired!");
        return client;
    }
}
