package dk.edu.mikkel.nfc;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NDEFEraser extends AppCompatActivity {

    private NfcAdapter mAdapter;
    private AlertDialog mDialog;
    private PendingIntent mPendingIntent;
    private Button cleanButton;
    private Button addButton;
    private Button deleteButton;
    private TextView formatText;
    private TextView ndefText;
    private Tag myTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndeferaser);

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage("Error", "This device does not support NFC");
            finish();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        cleanButton = (Button) (findViewById(R.id.erase_cleanButton));
        addButton = (Button) (findViewById(R.id.erase_addButton));
        deleteButton = (Button) (findViewById(R.id.erase_deleteButton));
        ndefText = (TextView) (findViewById(R.id.erase_ndefFormated));
        formatText = (TextView) (findViewById(R.id.erase_ndefNotFormated));
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
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(100);
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(NdefFormatable.get(myTag ) != null){
                    addButton.setAlpha(1);
                    addButton.setClickable(true);
                    formatText.setTextColor(Color.parseColor("#000000"));
                deleteButton.setAlpha(0.5f);
                deleteButton.setClickable(false);
                cleanButton.setAlpha(0.5f);
                cleanButton.setClickable(false);
                ndefText.setTextColor(Color.parseColor("#808080"));
            } else if( Ndef.get(myTag ) != null){
                addButton.setAlpha(0.5f);
                addButton.setClickable(false);
                formatText.setTextColor(Color.parseColor("#808080"));
                    deleteButton.setAlpha(1);
                    deleteButton.setClickable(true);
                    cleanButton.setAlpha(1);
                    cleanButton.setClickable(true);
                    ndefText.setTextColor(Color.parseColor("#000000"));
            } else {
                Toast.makeText(this, "No NDEF support", Toast.LENGTH_SHORT).show();
                addButton.setAlpha(0.5f);
                addButton.setClickable(false);
                formatText.setTextColor(Color.parseColor("#808080"));
                deleteButton.setAlpha(0.5f);
                deleteButton.setClickable(false);
                cleanButton.setAlpha(0.5f);
                cleanButton.setClickable(false);
                ndefText.setTextColor(Color.parseColor("#808080"));
            }
        }
    }
    public void cleanClick(View view){
        try {
            NdefRecord[] records = new NdefRecord[]{ createRecord("") };
            NdefMessage message = new NdefMessage(records);
            cleanTag(Ndef.get(myTag), message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void addClick(View view){
        try {
            NdefRecord[] records = new NdefRecord[]{ createRecord("") };
            NdefMessage message = new NdefMessage(records);
            formatTag(NdefFormatable.get(myTag), message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void deleteClick(View view){
            deleteFormating(Ndef.get(myTag));
    }

    private void cleanTag(Ndef ndef, NdefMessage msg){
        try {
            ndef.connect();

            try {
                ndef.writeNdefMessage(msg);
            }
            catch (Exception e) {
                e.printStackTrace();
                // let the user know the tag refused to format
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            // let the user know the tag refused to connect
        }
        finally {
            try {
                ndef.close();
                Toast.makeText(this, "Clean tag success", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void formatTag(NdefFormatable formatable, NdefMessage msg){
            try {
                formatable.connect();

                try {
                    formatable.format(msg);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    // let the user know the tag refused to format
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                // let the user know the tag refused to connect
            }
            finally {
                try {
                    formatable.close();
                    Toast.makeText(this, "Format tag success", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    private void deleteFormating (Ndef ndefTag) {
        if(ndefTag != null){
            try {
                ndefTag.connect();
                ndefTag.writeNdefMessage(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)));
                ndefTag.close();
                Toast.makeText(this, "Tag formatting deleted", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Lost connection to the tag", Toast.LENGTH_SHORT).show();
        }

    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImageView img = (ImageView)findViewById(R.id.erase_searching);
            AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
            frameAnimation.start();
        }
    }

    public void clickTag(View view){
        ImageView img = (ImageView)findViewById(R.id.erase_searching);
        AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();
        if(frameAnimation.isRunning()){
            frameAnimation.stop();
        } else {
            frameAnimation.start();
        }
    }
}
