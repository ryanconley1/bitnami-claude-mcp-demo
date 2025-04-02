package com.bitnami.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.bitnami.mcp.client.OciClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OciClientTests {

    @Autowired
    private OciClient ociClient;

    @Test
    public void testClient() {
        assertNotNull(ociClient);
    }

    @Test
    public void testPullIndex() throws Exception{
        ociClient.pullIndex();
    }
}
