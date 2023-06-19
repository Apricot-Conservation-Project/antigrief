package main;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.prefs.Preferences;
import java.math.BigDecimal;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public class AntigriefMain extends Plugin {
    private final int dailyQueryLimit = 450;

    private final DBInterface db = new DBInterface();

    private float startTime = System.currentTimeMillis();
    private float realTime;
    private int seconds;
    private RTInterval checkHourInterval = new RTInterval(300);

    private Preferences prefs;
    private int hourlyQueries;

    private int hour = Calendar.getInstance().get(Calendar.HOUR);
    private final HttpClient client = HttpClient.newHttpClient();

    private void try_thresh(float score, Player p) {
        if (score > 0.995) {
            Log.info("kicking @ (@ / @); intel: @", p.plainName(), p.ip(), p.uuid(), score);
            p.kick("[accent]You are using a VPN/Proxy! Please connect using your normal IP! If you think this is a mistake, ask for a whitelist at [white]apricotalliance.org[].");
        }
    }

    public void init() {
        // CREATE TABLE scores ( `ip` INTEGER UNSIGNED, `score` numeric(4, 3) );
        db.connect("ips", System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        if (System.getenv("DB_USER") == null) {
            Log.err("Set the env variables DB_USER and DB_PASSWORD");
            System.exit(1);
        }
        prefs = Preferences.userRoot().node(this.getClass().getName());
        hourlyQueries = prefs.getInt("ipQueries", 0);

        Events.run(EventType.Trigger.update, () -> {
            realTime = System.currentTimeMillis() - startTime;
            seconds = (int) (realTime / 1000);

            if (checkHourInterval.get(seconds)) {
                if (Calendar.getInstance().get(Calendar.HOUR) != hour) {
                    hour = Calendar.getInstance().get(Calendar.HOUR);
                    hourlyQueries = 0;
                }
            }
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            String ip = "INET_ATON('" + event.player.ip() + "')";
            HashMap<String, Object> entries = db.loadRow("scores", "ip", ip);
            if (entries != null) {
                try_thresh(((BigDecimal) entries.get("score")).floatValue(), event.player);
                return;
            }

            if (hourlyQueries > dailyQueryLimit / 24) {
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://check.getipintel.net/check.php?ip=" + event.player.ip() + "&contact=aa.mindustry@gmail.com"))
                    .GET() // GET is default
                    .build();

            HttpResponse<String> response = null;
            try {
                response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                hourlyQueries++;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            assert response != null;
            float score = Float.parseFloat(response.body());
            if (score < 0 || score > 1) {
                Log.err("getipintel RETURNED ERROR: " + score);
                return;
            }
            String[] keys = { "ip", "score" };
            Object[] vals = { ip, score };
            db.addEmptyRow("scores", keys, vals);
            try_thresh(score, event.player);
        });
    }
}
