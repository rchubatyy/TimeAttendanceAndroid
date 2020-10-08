package app.olivs.OnTime.View;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class OurLogoAndInfo extends AppCompatImageView {


    public OurLogoAndInfo(@NonNull final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://olivs.app/ontime"));
                context.startActivity(browserIntent);
            }
        });
    }
}
