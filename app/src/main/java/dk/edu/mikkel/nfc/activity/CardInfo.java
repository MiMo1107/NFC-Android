package dk.edu.mikkel.nfc.activity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvParser;
import com.github.devnied.emvnfccard.parser.IProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;

import dk.edu.mikkel.nfc.R;
import dk.edu.mikkel.nfc.model.Provider;

public class CardInfo extends AppCompatActivity {

    private NfcAdapter mAdapter;
    private AlertDialog mDialog;
    private PendingIntent mPendingIntent;
    private TextView number, date, type, pin, aid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_info);

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage("Error", "This device does not support NFC");
            finish();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        number = (TextView) (findViewById(R.id.card_number));
        date = (TextView) (findViewById(R.id.card_date));
        type = (TextView) (findViewById(R.id.card_type));
        pin = (TextView) (findViewById(R.id.card_pin));
        aid = (TextView) (findViewById(R.id.card_aid));
    }

    private void showMessage(String title, String message) {
        mDialog.setTitle(title);
        mDialog.setMessage(message);
        mDialog.show();
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(IsoDep.get(myTag) != null){
                Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(100);
                Toast.makeText(this, "Wait while reading data...", Toast.LENGTH_SHORT).show();
                getCardInfo(IsoDep.get(myTag));
            } else {
                Toast.makeText(this, "Can only read ISO tags", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCardInfo(IsoDep isoDep){
        try {
            Provider prov = new Provider();
            isoDep.connect();
            prov.setmTagCom(isoDep);
            EmvParser parser = new EmvParser(prov, true);
            EmvCard card = parser.readEmvCard();
            if(card != null){
                number.setText(card.getCardNumber());
                Log.d("cardnumber",card.getCardNumber());
                date.setText("" + new SimpleDateFormat("MM/yyyy").format(card.getExpireDate()));
                Log.d("date","" + card.getExpireDate());
                type.setText(card.getApplicationLabel());
                Log.d("type","" + card.getApplicationLabel());
                pin.setText("" + card.getLeftPinTry());
                Log.d("pin","" + card.getLeftPinTry());
                aid.setText(card.getAid());
                Log.d("aid","" + card.getAid());
                Toast.makeText(this, "Done reading" , Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lost connection", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lost connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("NFC is not enabled. Please go to the wireless settings to enable it.");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }

}
