package pers.pandora.test;

import pers.pandora.annotation.Bean;
import pers.pandora.annotation.Configruation;
import pers.pandora.servlet.TestServlet;

@Configruation
public class TestClientConfig {

    @Bean
    public TestServlet testServlet(){
        TestServlet testServlet = new TestServlet();
        return testServlet;
    }

    @Bean
    public Client client(){
        Client client = new Client();
        client.setTestOM("test @Bean Client autowired!");
        return client;
    }
}
