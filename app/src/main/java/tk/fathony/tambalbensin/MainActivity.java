package tk.fathony.tambalbensin;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String HOST = "http://pombensin.tk/";
    private static final String URL_POM = "pom.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void mapPom(View v){
        Intent in = new Intent(this,MapsActivity.class);
        in.putExtra("url",HOST+URL_POM);
        in.putExtra("jenis","pom");
        startActivity(in);
    }
}
