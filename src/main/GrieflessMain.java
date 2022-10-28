package main;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Calendar;
import java.util.HashMap;
import java.util.prefs.Preferences;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.mod.Plugin;


public class GrieflessMain extends Plugin {

    private final float threshold = 0.99f;
    private final int dailyQueryLimit = 450;

    private final DBInterface db = new DBInterface();

    private float startTime = System.currentTimeMillis();
    private float realTime;
    private int seconds;
    private RTInterval checkHourInterval = new RTInterval(300);

    private Preferences prefs;
    private int hourlyQueries;

    private int hour = Calendar.getInstance().get(Calendar.HOUR);

    public void init(){

        db.connect("ips", "recessive", "8N~hT4=a\"M89Gk6@");

        prefs = Preferences.userRoot().node(this.getClass().getName());
        hourlyQueries = prefs.getInt("ipQueries", 0);

        Events.on(EventType.Trigger.class, event ->{
            realTime = System.currentTimeMillis() - startTime;
            seconds = (int) (realTime / 1000);

            if(checkHourInterval.get(seconds)){
                if(Calendar.getInstance().get(Calendar.HOUR) != hour){
                    hour = Calendar.getInstance().get(Calendar.HOUR);
                    hourlyQueries = 0;
                }
            }
        });

        Events.on(EventType.PlayerConnect.class, event ->{
            String ip = event.player.ip();
            if(db.hasRow("scores", "ip", ip)){
                HashMap<String, Object> entries = db.loadRow("scores", "ip", ip);
                if((float) entries.get("score") > threshold){
                    event.player.kick("You are using a VPN/Proxy! Please connect using your normal IP!");
                }
                return;
            }

            if(hourlyQueries > dailyQueryLimit/24){
                return;
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://check.getipintel.net/check.php?ip=" + ip + "&contact=aa.mindustry@gmail.com"))
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
            if(score < 0 || score > 1){
                Log.err("getipintel RETURNED ERROR: " + score);
                return;
            }

            db.addEmptyRow("scores", "ip", ip);

            db.saveRow("scores", "ip", ip, "score", score);

            if(score > threshold){
                event.player.kick("You are using a VPN/Proxy! Please connect using your normal IP!");
            }
        });

    }
}
