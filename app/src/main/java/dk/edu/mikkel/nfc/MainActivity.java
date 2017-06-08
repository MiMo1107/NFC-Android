package dk.edu.mikkel.nfc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void readNFC(View view){
        Intent intent = new Intent(this,NFCReader.class);
        startActivity(intent);
    }

    public void writeNFC(View view){
        Intent intent = new Intent(this,NFCWriter.class);
        startActivity(intent);
    }
}
