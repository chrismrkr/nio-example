package util;

import org.example.server.config.ServerConfig;
import org.example.util.ConfigLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ConfigLoaderTest {

    @Test
    public void 서버설정_로딩_성공() throws IOException {
        try {
            // given
            String pathFile = "config.json";
            // when
            ServerConfig load = ConfigLoader.load(pathFile, ServerConfig.class);
            // then
            Assert.assertNotNull(load);
        } catch (Exception e) {
            Assert.assertEquals(0, 1);
        }
    }
}
