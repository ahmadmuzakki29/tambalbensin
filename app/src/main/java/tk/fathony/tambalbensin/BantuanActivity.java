package tk.fathony.tambalbensin;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.TextView;

public class BantuanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bantuan);
        ActionBar ab = getSupportActionBar();
        if(ab!=null){
            ab.setTitle("Bantuan");
        }

        String bantuan = "Pilih menu POM BENSIN untuk mencari pom pengisian bahan bakar terdekat dari lokasi anda. Setelah menemukan silahkan menekan tanda ";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(bantuan);
        builder.setSpan(new ImageSpan(this, R.drawable.marker_pom),
                builder.length() - 1, builder.length(), 0);

        ((TextView) findViewById(R.id.txtBantuanPom)).setText(builder);

        bantuan = "Pilih menu TAMBAL BAN untuk mencari tukang tambal ban terdekat dari lokasi anda. Setelah menemukan silahkan menekan tanda ";
        builder = new SpannableStringBuilder();
        builder.append(bantuan);
        builder.setSpan(new ImageSpan(this, R.drawable.marker_ban),
                builder.length() - 1, builder.length(), 0);

        ((TextView) findViewById(R.id.txtBantuanBan)).setText(builder);

        bantuan = "Setelah menekan marker maka akan muncul keterangan dari lokasi yang bersangkutan. Anda dapat menekan tombol  ";
        builder = new SpannableStringBuilder();
        builder.append(bantuan);
        builder.setSpan(new ImageSpan(this, R.drawable.gohere),
                builder.length() - 1, builder.length(), 0);
        builder.append(" untuk mencari rute terdekat dari lokasi anda yang ditandai oleh  ");
        builder.setSpan(new ImageSpan(this, R.drawable.marker_me),
                builder.length() - 1, builder.length(), 0);

        ((TextView) findViewById(R.id.txtgotohere)).setText(builder);

    }
}
