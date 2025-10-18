package org.news.api.firebase;



import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;

public class FirebaseInitializer {

    public static void init() throws Exception {
        if (FirebaseApp.getApps().isEmpty()) { // Only init once
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/serviceAccountKey.json"); // Update path!
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://cinepulse-54397-default-rtdb.asia-southeast1.firebasedatabase.app/") // Your Realtime DB URL from Console
                    .build();
            FirebaseApp.initializeApp(options);

        }
    }
}
