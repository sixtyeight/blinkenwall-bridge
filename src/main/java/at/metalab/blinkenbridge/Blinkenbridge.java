package at.metalab.blinkenbridge;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@Controller
public class Blinkenbridge {

	private final static Log LOG = LogFactory.getLog(Blinkenbridge.class);

	private volatile WebSocketClient client;

	public static void main(String[] args) {
		SpringApplication.run(Blinkenbridge.class, args);
	}

	@GetMapping()
	public ResponseEntity<String> turnOff() {
		try {
			LOG.info("entered turnOff()");
			String message = "{ \"cmd\": \"turnoff\", \"req\":\"" + uuid() + "\", \"source\": \"blinkenbridge\" }";

			LOG.info("sending message: " + message);
			getClient().send(message);
		} catch (URISyntaxException e) {
			// can't happen
		} catch (InterruptedException e) {
			LOG.error("turnoff failed: {}", e);
			return ResponseEntity.status(503).body("connection to blinkenwall websocket failed");
		} catch (Exception e) {
			LOG.error("turnoff failed: {}", e);
			return ResponseEntity.status(500).body(e.getMessage());
		}

		return ResponseEntity.ok("ok");
	}

	private String uuid() {
		return UUID.randomUUID().toString();
	}

	private synchronized WebSocketClient createClient() throws URISyntaxException, InterruptedException {
		URI uri = new URI("ws://blinkenwall.com:1337/blinkenwall");

		WebSocketClient client = new WebSocketClient(uri) {

			@Override
			public void onOpen(ServerHandshake arg0) {
				LOG.info("connection openend");
			}

			@Override
			public void onMessage(String arg0) {
				LOG.info("received message: " + arg0);
			}

			@Override
			public void onError(Exception arg0) {
				LOG.error("received error: {}", arg0);
			}

			@Override
			public void onClose(int arg0, String arg1, boolean arg2) {
				LOG.info("connection closed");
				Blinkenbridge.this.client = null;
			}
		};

		client.connectBlocking(10, TimeUnit.SECONDS);
		this.client = client;

		return client;
	}

	private WebSocketClient getClient() throws URISyntaxException, InterruptedException {
		if (client == null || !client.isOpen()) {
			this.client = createClient();
		}

		return this.client;
	}

}
