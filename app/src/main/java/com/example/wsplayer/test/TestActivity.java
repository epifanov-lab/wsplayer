package com.example.wsplayer.test;

import android.os.Bundle;
import android.view.TextureView;

import com.example.wsplayer.R;
import com.example.wsplayer.utils.schedulers.Schedulers;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.logging.HttpLoggingInterceptor;
import ru.realtimetech.webka.client.Client;


public class TestActivity extends AppCompatActivity {

  private TextureView mTexture1, mTexture2;
  private Client mClient;
  private TempStorage mStorage;

  private HttpLoggingInterceptor.Logger mLogger = HttpLoggingInterceptor.Logger.DEFAULT;

  private final List<Player> mPlayers = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test);

    mTexture1 = findViewById(R.id.texture_1);
    mTexture2 = findViewById(R.id.texture_2);

    mStorage = new TempStorage();
    mClient = Client.builder().config(TestUtils.CONFIG_PROD)
                    .data(mStorage::put, mStorage::get)
                    .verbose().build();

    Translation.videoMain(mClient, 0, 1)
               .transform(Schedulers::work_main)
               .subscribe((translations -> {
                 Translation translation = translations[0];
                 System.out.println("Translation.videoMain(0, 1)\n" + translation);
                 TestUtils.testRunHlsPlayer(TestActivity.this, mTexture1, translation.streamMediaUrl);
               }));

    Translation.bySessionId(mClient, "3Pr8PdMMXR")
               .transform(Schedulers::work_main)
               .subscribe((translation -> {
                 System.out.println("Translation.bySessionId(3Pr8PdMMXR)\n" + translation);
                 TestActivity context = TestActivity.this;
                 TestUtils.testRunWsPlayer(context, TestUtils.http(context, mLogger), mLogger, mTexture2, translation);
               }));
  }

}
