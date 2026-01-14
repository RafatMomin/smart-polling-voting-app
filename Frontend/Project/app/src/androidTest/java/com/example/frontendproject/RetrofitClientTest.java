import org.junit.Test;
import static org.junit.Assert.*;
import com.example.frontendproject.net.RetrofitClient;
import com.example.frontendproject.net.ChatApi;
import retrofit2.Retrofit;

public class RetrofitClientTest {

    @Test
    public void get_returnsSingletonInstance() {
        Retrofit first = RetrofitClient.get();
        Retrofit second = RetrofitClient.get();
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void chat_returnsChatApiInstance() {
        ChatApi api = RetrofitClient.chat();
        assertNotNull(api);
        assertTrue(api instanceof ChatApi);
    }
}
